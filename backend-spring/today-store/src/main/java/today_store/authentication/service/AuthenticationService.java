package today_store.authentication.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import today_store.authentication.dto.LoginRequest;
import today_store.authentication.dto.LoginResponse;
import today_store.authentication.dto.RefreshTokenRequest;
import today_store.authentication.dto.RefreshTokenResponse;
import today_store.authentication.entity.BlacklistedToken;
import today_store.authentication.entity.RefreshToken;
import today_store.authentication.entity.User;
import today_store.authentication.jwt.JwtTokenProvider;
import today_store.authentication.oauth2.OAuth2UserInfo;
import today_store.authentication.oauth2.OAuth2UserInfoFactory;
import today_store.authentication.exception.InvalidOauthCodeException;
import today_store.authentication.exception.InvalidRefreshTokenException;
import today_store.authentication.exception.UserDisabledException;
import today_store.authentication.exception.UserNotFoundException;
import today_store.authentication.exception.UnsupportedProviderException;
import today_store.authentication.repository.BlacklistedTokenRepository;
import today_store.authentication.repository.RefreshTokenRepository;
import today_store.authentication.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class AuthenticationService {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final WebClient webClient;

    public LoginResponse oauthLogin(LoginRequest loginRequest) {
        log.info("OAuth login attempt for provider: {}", loginRequest.getProvider());

        ClientRegistration clientRegistration = Optional.ofNullable(clientRegistrationRepository.findByRegistrationId(loginRequest.getProvider()))
                .orElseThrow(() -> {
                    log.error("Unsupported provider: {}", loginRequest.getProvider());
                    return new UnsupportedProviderException();
                });

        log.debug("Found client registration for: {}", clientRegistration.getClientName());

        String token = getAccessToken(clientRegistration, loginRequest.getCode());
        Map<String, Object> userAttributes = getUserAttributes(clientRegistration, token);
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(loginRequest.getProvider(), userAttributes);

        String maskedEmail = oAuth2UserInfo.getEmail().replaceAll("(?<=.{3}).(?=.*@)", "*");
        log.debug("OAuth2 User authenticated. Email: {}, Provider: {}", maskedEmail, loginRequest.getProvider());

        boolean isNewUser = userRepository.findByProviderAndProviderId(loginRequest.getProvider(), oAuth2UserInfo.getId()).isEmpty();
        if (isNewUser) {
            log.info("New user detected: {}", maskedEmail);
        } else {
            log.info("Existing user detected: {}", maskedEmail);
        }
        User user = saveOrUpdate(oAuth2UserInfo, loginRequest.getProvider());

        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), null);
        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);
        log.debug("Access token and refresh token created for user: {}", user.getId());

        // Save refresh token to Redis
        refreshTokenRepository.save(new RefreshToken(user.getId(), refreshToken));
        log.info("Refresh token saved in Redis for user ID: {}", user.getId());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(LoginResponse.UserDetails.from(user, isNewUser))
                .build();
    }

    @Transactional
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        log.info("Attempting to refresh token");

        // 1. Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Invalid refresh token received");
            throw new InvalidRefreshTokenException();
        }

        // 2. Extract user email and find user in DB
        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> {
                    log.error("User not found during token refresh: {}", authentication.getName());
                    return new UserNotFoundException();
                });

        // 3. Verify refresh token in Redis
        RefreshToken storedToken = refreshTokenRepository.findById(user.getId())
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in storage for user ID: {}", user.getId());
                    return new InvalidRefreshTokenException();
                });

        if (!storedToken.getRefreshToken().equals(refreshToken)) {
            log.warn("Mismatched refresh token. Potential reuse attack. User ID: {}", user.getId());
            throw new InvalidRefreshTokenException();
        }
        log.debug("Refresh token validated for user: {}", user.getId());

        // 4. Generate new tokens (rotation)
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(authentication);
        log.debug("New access and refresh tokens generated for user: {}", user.getId());

        // 5. Update refresh token in Redis
        refreshTokenRepository.save(new RefreshToken(user.getId(), newRefreshToken));
        log.info("New refresh token saved in Redis for user ID: {}", user.getId());

        // 6. Return new tokens
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    @Transactional
    public void logout(String accessToken, String email) {
        String maskedEmail = email.replaceAll("(?<=.{3}).(?=.*@)", "*");
        log.info("Logout request for user: {}", maskedEmail);

        // Blacklist the access token
        Long remainingExpiration = jwtTokenProvider.getRemainingExpirationMillis(accessToken);
        if (remainingExpiration > 0) {
            blacklistedTokenRepository.save(new BlacklistedToken(accessToken, "logout", remainingExpiration));
            log.info("Access token blacklisted for user: {}", maskedEmail);
        }

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user != null) {
            refreshTokenRepository.deleteById(user.getId());
            log.info("Refresh token deleted for user: {}", maskedEmail);
        } else {
            log.warn("User not found for logout: {}", maskedEmail);
        }
    }

    @Transactional
    public void deleteUser(String accessToken, String email) {
        String maskedEmail = email.replaceAll("(?<=.{3}).(?=.*@)", "*");
        log.info("Account deletion request for user: {}", maskedEmail);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found during account deletion: {}", maskedEmail);
                    return new UserNotFoundException();
                });

        if (Boolean.FALSE.equals(user.getIsActive())) {
            log.warn("Account deletion requested for already deactivated user: {}", maskedEmail);
            throw new UserDisabledException();
        }

        UUID userId = user.getId();

        // 1. Delete refresh token from Redis
        refreshTokenRepository.deleteById(userId);
        log.info("Refresh token deleted during account deletion for user ID: {}", userId);

        // 2. Blacklist current access token to invalidate it immediately
        Long remainingExpiration = jwtTokenProvider.getRemainingExpirationMillis(accessToken);
        if (remainingExpiration > 0) {
            blacklistedTokenRepository.save(new BlacklistedToken(accessToken, "delete_account", remainingExpiration));
            log.info("Access token blacklisted for deleted user ID: {}", userId);
        }

        // 3. Soft delete
        user.deactivate();
        log.info("User account successfully deactivated (soft delete). User ID: {}", userId);
    }

    private String getAccessToken(ClientRegistration clientRegistration, String code) {
        log.debug("Requesting access token from provider endpoint: {}", clientRegistration.getProviderDetails().getTokenUri());
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientRegistration.getClientId());
        formData.add("client_secret", clientRegistration.getClientSecret());
        formData.add("redirect_uri", clientRegistration.getRedirectUri());
        formData.add("code", code);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(clientRegistration.getProviderDetails().getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(formData)
                    .retrieve()
                    .onStatus(org.springframework.http.HttpStatusCode::is4xxClientError, clientResponse -> {
                        log.error("Client error while getting access token: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .map(body -> new InvalidOauthCodeException());
                    })
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            String accessToken = (String) Optional.ofNullable(response)
                    .map(r -> r.get("access_token"))
                    .orElseThrow(() -> {
                        log.error("Access token not found in provider response");
                        return new InvalidOauthCodeException();
                    });
            log.debug("Access token received successfully.");
            return accessToken;
        } catch (Exception e) {
            log.error("Error communicating with OAuth provider: ", e);
            if (e instanceof InvalidOauthCodeException) throw (InvalidOauthCodeException) e;
            throw new InvalidOauthCodeException();
        }
    }

    private Map<String, Object> getUserAttributes(ClientRegistration clientRegistration, String token) {
        log.info("Requesting user attributes from {}", clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri());
        try {
            Map<String, Object> userAttributes = webClient.get()
                    .uri(clientRegistration.getProviderDetails().getUserInfoEndpoint().getUri())
                    .headers(header -> header.setBearerAuth(token))
                    .retrieve()
                    .onStatus(org.springframework.http.HttpStatusCode::isError, clientResponse -> {
                        log.error("Error while getting user attributes: {}", clientResponse.statusCode());
                        return clientResponse.bodyToMono(String.class)
                                .map(body -> new InvalidOauthCodeException());
                    })
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

            if (userAttributes == null) {
                log.error("User attributes response is null");
                throw new InvalidOauthCodeException();
            }

            log.debug("User attributes received successfully.");
            return userAttributes;
        } catch (Exception e) {
            log.error("Error getting user attributes: ", e);
            if (e instanceof InvalidOauthCodeException) throw (InvalidOauthCodeException) e;
            throw new InvalidOauthCodeException();
        }
    }

    private User saveOrUpdate(OAuth2UserInfo oAuth2UserInfo, String provider) {
        String email = oAuth2UserInfo.getEmail();
        if (email == null || email.isBlank()) {
            email = oAuth2UserInfo.getId() + "@" + provider + ".com";
            log.info("Email not provided by {}, using fallback: {}", provider, email);
        }

        final String finalEmail = email;

        User user = userRepository.findByProviderAndProviderId(provider, oAuth2UserInfo.getId())
                .map(entity -> {
                    log.info("Updating last login time for user: {}", entity.getId());
                    entity.recordLogin();
                    if (Boolean.FALSE.equals(entity.getIsActive())) {
                        log.warn("Login attempt for deactivated user: {}", entity.getId());
                        throw new UserDisabledException();
                    }
                    return entity;
                })
                .orElseGet(() -> {
                    log.info("Creating new user with email: {}",oAuth2UserInfo.getId() );
                    return new User(
                            finalEmail,
                            oAuth2UserInfo.getName(),
                            provider,
                            oAuth2UserInfo.getId(),
                            oAuth2UserInfo.getImageUrl()
                    );
                });

        user.recordLogin();

        return userRepository.save(user);
    }


}
