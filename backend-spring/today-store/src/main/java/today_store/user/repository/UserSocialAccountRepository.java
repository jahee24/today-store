package today_store.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import today_store.authentication.entity.User;
import today_store.user.entity.UserSocialAccount;

import java.util.Optional;
import java.util.UUID;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {
    Optional<UserSocialAccount> findByUserAndPlatform(User user, String platform);
    Optional<UserSocialAccount> findByPlatformAndSocialUserId(String platform, String socialUserId);
}
