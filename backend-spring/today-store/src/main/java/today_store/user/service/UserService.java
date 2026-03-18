package today_store.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import today_store.authentication.entity.User;
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

        user.updateName(request.getName());

        User updatedUser = userRepository.saveAndFlush(user);

        return UpdateUserProfileResponse.from(updatedUser);
    }
}

