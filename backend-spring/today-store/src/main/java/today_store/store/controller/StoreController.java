package today_store.store.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import today_store.authentication.entity.User;
import today_store.authentication.repository.UserRepository;
import today_store.common.exception.CustomException;
import today_store.common.exception.ErrorCode;
import today_store.common.ratelimit.RateLimit;
import today_store.common.ratelimit.RateLimitTier;
import today_store.store.dto.*;
import today_store.store.service.StoreService;
import today_store.user.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/stores")
public class StoreController {

    private final StoreService storeService;
    private final UserService userService;

    @PostMapping
    @RateLimit(tier = RateLimitTier.MIDDLE)
    @ResponseStatus(HttpStatus.CREATED)
    public CreateStoreResponse createStore(@Valid @RequestBody CreateStoreRequest request, Authentication authentication) {
        User user = userService.getUser(authentication);
        return storeService.createStore(user, request);
    }

    @GetMapping("/me")
    @RateLimit(tier = RateLimitTier.LOW)
    public StoreResponse getMyStore(Authentication authentication) {
        User user = userService.getUser(authentication);
        return storeService.getStore(user);
    }

    @PatchMapping("/me")
    @RateLimit(tier = RateLimitTier.MIDDLE)
    public UpdateStoreResponse updateMyStore(@RequestBody UpdateStoreRequest request, Authentication authentication) {
        if (authentication == null) {
            throw new CustomException(ErrorCode.AUTHENTICATION_REQUIRED);
        }
        return storeService.updateStore(authentication.getName(), request);
    }
}
