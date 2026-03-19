package today_store.content.request.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import today_store.authentication.entity.User;
import today_store.common.ratelimit.RateLimit;
import today_store.common.ratelimit.RateLimitTier;
import today_store.content.request.dto.*;
import today_store.content.request.service.GenerationRequestService;
import today_store.user.service.UserService;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/contents")
public class GenerationRequestController {

    private final GenerationRequestService requestService;
    private final UserService userService;

    @PostMapping("/request")
    @RateLimit(tier = RateLimitTier.MIDDLE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreateGenerationResponse createRequest(
            @Valid @ModelAttribute CreateGenerationRequest request,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return requestService.createRequest(user, request);
    }

    @GetMapping("/requests")
    @RateLimit(tier = RateLimitTier.LOW)
    public GenerationRequestListResponse getRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return requestService.getRequests(user, page, size);
    }

    @GetMapping("/request/{requestId}")
    @RateLimit(tier = RateLimitTier.LOW)
    public GenerationRequestDetailResponse getRequestDetail(
            @PathVariable UUID requestId,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        return requestService.getRequestDetail(user, requestId);
    }

    @PatchMapping("/request/{requestId}")
    @RateLimit(tier = RateLimitTier.MIDDLE)
    public UpdateGenerationResponse updateRequest(
            @PathVariable UUID requestId,
            @Valid @ModelAttribute UpdateGenerationRequest request,
            Authentication authentication) {

        return requestService.updateRequest(authentication.getName(), requestId, request);
    }

    @DeleteMapping("/{requestId}")
    @RateLimit(tier = RateLimitTier.MIDDLE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequest(
            @PathVariable UUID requestId,
            Authentication authentication) {

        User user = userService.getUser(authentication);
        requestService.deleteRequest(user, requestId);
    }
}