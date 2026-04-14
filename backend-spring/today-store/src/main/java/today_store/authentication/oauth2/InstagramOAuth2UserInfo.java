package today_store.authentication.oauth2;

import java.util.Map;

public class InstagramOAuth2UserInfo extends OAuth2UserInfo {

    public InstagramOAuth2UserInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("username");
    }

    @Override
    public String getEmail() {
        // Instagram Graph API does not provide user's email.
        return null;
    }

    @Override
    public String getImageUrl() {
        // Basic Instagram Display API doesn't return profile picture URL by default.
        return null;
    }
}
