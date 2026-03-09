package today_store.authentication.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import today_store.authentication.dto.LoginRequest;
import today_store.authentication.dto.LoginResponse;
import today_store.authentication.dto.RefreshTokenRequest;
import today_store.authentication.dto.RefreshTokenResponse;
import today_store.authentication.service.AuthenticationService;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.ratelimit.RateLimit;
import today_store.common.ratelimit.RateLimitTier;

import jakarta.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationService authenticationService;

    @RateLimit(tier = RateLimitTier.HIGH)
    @PostMapping("/oauth/login")
    public ResponseEntity<LoginResponse> oauthLogin(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authenticationService.oauthLogin(loginRequest);
        return ResponseEntity.ok(response);
    }

    @RateLimit(tier = RateLimitTier.HIGH)
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        RefreshTokenResponse response = authenticationService.refreshToken(refreshTokenRequest);
        return ResponseEntity.ok(response);
    }

    @RateLimit(tier = RateLimitTier.HIGH)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader, Authentication authentication) {
        if (authentication != null && authorizationHeader != null) {
            String accessToken = resolveToken(authorizationHeader);
            if (accessToken != null) {
                authenticationService.logout(accessToken, authentication.getName());
                return ResponseEntity.noContent().build();
            }
        }
        throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
    }

    @RateLimit(tier = RateLimitTier.HIGH)
    @DeleteMapping("/me")
    public ResponseEntity<Void> delete(@RequestHeader("Authorization") String authorizationHeader, Authentication authentication) {
        if (authentication != null && authorizationHeader != null) {
            String accessToken = resolveToken(authorizationHeader);
            if (accessToken != null) {
                authenticationService.deleteUser(accessToken, authentication.getName());
                return ResponseEntity.noContent().build();
            }
        }
        throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
    }

    private String resolveToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}