package today_store.common.instagram.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import today_store.authentication.entity.User;
import today_store.common.dto.ApiResponse;
import today_store.common.instagram.dto.InstagramAuthRequest;
import today_store.common.instagram.dto.InstagramAuthResponse;
import today_store.common.instagram.dto.InstagramPublishRequest;
import today_store.common.instagram.dto.InstagramPublishResponse;
import today_store.common.instagram.service.InstagramService;
import today_store.user.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/instagram")
public class InstagramController {

    private final InstagramService instagramService;
    private final UserService userService;

    @PostMapping("/auth")
    public ApiResponse<InstagramAuthResponse> authenticate(
            @Valid @RequestBody InstagramAuthRequest request,
            Authentication authentication) {
        User user = userService.getUser(authentication);
        InstagramAuthResponse response = instagramService.authenticate(user, request);
        return ApiResponse.success(response);
    }

    @PostMapping("/publish")
    public InstagramPublishResponse publish(
            @Valid @RequestBody InstagramPublishRequest request,
            Authentication authentication) {
        User user = userService.getUser(authentication);
        return instagramService.publish(user, request);
    }
}
