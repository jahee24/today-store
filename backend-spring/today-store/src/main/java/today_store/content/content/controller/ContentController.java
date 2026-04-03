package today_store.content.content.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import today_store.authentication.entity.User;
import today_store.common.ratelimit.RateLimit;
import today_store.common.ratelimit.RateLimitTier;
import today_store.content.content.dto.*;
import today_store.content.content.service.ContentService;
import today_store.user.service.UserService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/contents")
public class ContentController {

    private final ContentService contentService;
    private final UserService userService;

    @PostMapping("/{requestId}/generate")
    @RateLimit(tier = RateLimitTier.HIGH)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerateContentResponse generateContent(
            @PathVariable UUID requestId,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return contentService.startGeneration(user, requestId);
    }

    @GetMapping("/task/{apiLogId}")
    @RateLimit(tier = RateLimitTier.LOW)
    public TaskStatusResponse getTaskStatus(
            @PathVariable UUID apiLogId,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return contentService.getTaskStatus(user, apiLogId);
    }

    @GetMapping("/{requestId}/contents")
    @RateLimit(tier = RateLimitTier.LOW)
    public ContentListResponse getContentList(
            @PathVariable UUID requestId,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return contentService.getContentList(user, requestId);
    }

    @GetMapping("/{contentId}")
    @RateLimit(tier = RateLimitTier.LOW)
    public ContentResponse getContent(
            @PathVariable UUID contentId,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return contentService.getContent(user, contentId);
    }

    @PatchMapping("/{contentId}")
    @RateLimit(tier = RateLimitTier.MIDDLE)
    public ContentResponse updateContent(
            @PathVariable UUID contentId,
            @RequestBody UpdateContentRequest request,
            Authentication authentication) {

        return contentService.updateContent(authentication.getName(), contentId, request);
    }

    @PostMapping("/{contentId}/regenerate")
    @RateLimit(tier = RateLimitTier.HIGH)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public GenerateContentResponse regenerateContent(
            @PathVariable UUID contentId,
            @Valid @RequestBody RegenerateContentRequest request,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return contentService.startRegeneration(user, contentId, request);
    }

    @DeleteMapping("/{contentId}")
    @RateLimit(tier = RateLimitTier.MIDDLE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteContent(
            @PathVariable UUID contentId,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        contentService.deleteContent(user, contentId);
    }
}
