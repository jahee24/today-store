package today_store.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import today_store.authentication.entity.User;
import today_store.user.dto.UpdateUserProfileRequest;
import today_store.user.dto.UpdateUserProfileResponse;
import today_store.user.dto.UserProfileResponse;

@Service
@RequiredArgsConstructor
public class UserService {

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    @Transactional
    public UpdateUserProfileResponse updateUserProfile(User user, UpdateUserProfileRequest request) {
        user.updateName(request.getName());
        return UpdateUserProfileResponse.builder()
                .id(user.getId())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

