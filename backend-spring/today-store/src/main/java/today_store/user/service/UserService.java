package today_store.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import today_store.authentication.entity.RefreshToken;
import today_store.authentication.entity.User;
import today_store.authentication.jwt.JwtTokenProvider;
import today_store.authentication.repository.RefreshTokenRepository;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.user.dto.UpdateUserProfileRequest;
import today_store.user.dto.UpdateUserProfileResponse;
import today_store.user.dto.UserProfileResponse;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional(readOnly = true)
    public User getUser(Authentication authentication) {
        if (authentication == null) {
            throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(User user) {
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UpdateUserProfileResponse updateUserProfile(String email, UpdateUserProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        boolean emailChanged = false;
        String newEmail = request.getEmail();

        // 1. 이메일 변경 여부 확인 및 중복 검증
        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(email)) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.updateEmail(newEmail);
            emailChanged = true;
        }

        // 2. 이름 업데이트
        user.updateName(request.getName());

        // 3. 변경 사항 저장 (Flush를 통해 updatedAt 갱신 및 DB 반영 확인)
        User updatedUser = userRepository.saveAndFlush(user);

        String newAccessToken = null;
        String newRefreshToken = null;

        // 4. 이메일이 변경된 경우 인증 식별자(Subject)가 변경되었으므로 토큰 재발급
        if (emailChanged) {
            Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
            // 새로운 이메일을 Subject로 하는 새로운 인증 객체 생성
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    updatedUser.getEmail(),
                    null,
                    currentAuth != null ? currentAuth.getAuthorities() : null
            );

            newAccessToken = jwtTokenProvider.createAccessToken(newAuth);
            newRefreshToken = jwtTokenProvider.createRefreshToken(newAuth);

            // Redis에 저장된 Refresh Token 갱신
            refreshTokenRepository.save(new RefreshToken(updatedUser.getId(), newRefreshToken));
        }

        return UpdateUserProfileResponse.from(updatedUser, newAccessToken, newRefreshToken);
    }
}

