package today_store.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import today_store.authentication.entity.User;
import today_store.common.exception.CustomException;
import today_store.user.entity.UserSocialAccount;
import today_store.user.repository.UserSocialAccountRepository;
import today_store.common.exception.ErrorCode;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserSocialAccountService {

    private final UserSocialAccountRepository userSocialAccountRepository;

    @Transactional
    public void linkAccount(User user, String platform, String socialUserId, String username, String accessToken, LocalDateTime expiresAt) {
        // Check for conflicts: Is this social account already linked to ANOTHER user?
        Optional<UserSocialAccount> existingAccount = userSocialAccountRepository.findByPlatformAndSocialUserId(platform, socialUserId);
        if (existingAccount.isPresent() && !existingAccount.get().getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.INSTAGRAM_ALREADY_LINKED);
        }

        // Save or update UserSocialAccount for THIS user and platform
        UserSocialAccount socialAccount = userSocialAccountRepository.findByUserAndPlatform(user, platform)
                .orElseGet(() -> {
                    UserSocialAccount newAccount = new UserSocialAccount();
                    newAccount.setUser(user);
                    newAccount.setPlatform(platform);
                    return newAccount;
                });

        socialAccount.setSocialUserId(socialUserId);
        socialAccount.setUsername(username);
        socialAccount.setAccessToken(accessToken);
        socialAccount.setTokenExpiresAt(expiresAt);
        socialAccount.setIsValid(true);

        userSocialAccountRepository.save(socialAccount);
    }
}
