package today_store.content.publish.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import today_store.authentication.entity.User;
import today_store.common.ratelimit.RateLimit;
import today_store.common.ratelimit.RateLimitTier;
import today_store.content.publish.dto.*;
import today_store.content.publish.service.DeeplinkPublishService;
import today_store.content.publish.service.ManualPublishService;
import today_store.user.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/publish")
public class PublishController {

    private final ManualPublishService manualPublishService;
    private final DeeplinkPublishService deeplinkPublishService;
    private final UserService userService;

    @PostMapping("/manual/start")
    @RateLimit(tier = RateLimitTier.LOW)
    @ResponseStatus(HttpStatus.CREATED)
    public StartManualPublishResponse startManualPublish(
            @Valid @RequestBody StartManualPublishRequest request,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return manualPublishService.startManualPublish(user, request);
    }

    @PostMapping("/deeplink")
    @RateLimit(tier = RateLimitTier.LOW)
    @ResponseStatus(HttpStatus.CREATED)
    public DeeplinkPublishResponse generateDeeplink(
            @Valid @RequestBody DeeplinkPublishRequest request,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return deeplinkPublishService.generateDeeplink(user, request);
    }

    @PatchMapping("/complete")
    @RateLimit(tier = RateLimitTier.LOW)
    public CompleteManualPublishResponse completeManualPublish(
            @Valid @RequestBody CompleteManualPublishRequest request,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return manualPublishService.completeManualPublish(user, request);
    }
}
