package today_store.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth
    MISSING_REQUIRED_FIELDS(HttpStatus.BAD_REQUEST, "A001", "Missing required fields."),
    UNSUPPORTED_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "A002", "Unsupported oauth provider."),
    INVALID_OAUTH_CODE(HttpStatus.UNAUTHORIZED, "A003", "Invalid oauth code."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A004", "Invalid or expired refresh token."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "A005", "Full authentication is required."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "A006", "Access denied."),
    USER_DISABLED(HttpStatus.FORBIDDEN, "A006", "Access denied. Your account has been deactivated."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A007", "User not found."),


    // Rate Limit
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "R001", "Rate limit exceeded. Please try again later.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
