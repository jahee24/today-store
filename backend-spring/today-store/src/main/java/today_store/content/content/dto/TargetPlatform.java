package today_store.content.content.dto;

import today_store.content.content.exception.InvalidRegenerationRequestException;

public enum TargetPlatform {
    INSTAGRAM,
    KARROT,
    NAVER;

    public static TargetPlatform from(String value) {
        try {
            return TargetPlatform.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidRegenerationRequestException();
        }
    }
}
