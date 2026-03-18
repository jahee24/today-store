package today_store.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import today_store.authentication.entity.User;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.ratelimit.RateLimit;
import today_store.common.ratelimit.RateLimitTier;
import today_store.user.dto.UpdateUserProfileRequest;
import today_store.user.dto.UpdateUserProfileResponse;
import today_store.user.dto.UserProfileResponse;
import today_store.user.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @RateLimit(tier = RateLimitTier.LOW)
    public UserProfileResponse getMyProfile(Authentication authentication) {
        User user = userService.getUser(authentication);
        return userService.getUserProfile(user);
    }

    @PatchMapping("/me")
    @RateLimit(tier = RateLimitTier.MIDDLE)
    public UpdateUserProfileResponse updateMyProfile(@Valid @RequestBody UpdateUserProfileRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        return userService.updateUserProfile(authentication.getName(), request);
    }
}