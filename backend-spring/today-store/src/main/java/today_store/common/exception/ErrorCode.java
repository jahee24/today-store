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
    ACCESS_DENIED_TO_RESOURCE(HttpStatus.FORBIDDEN, "A008", "Access denied. Resource ownership mismatch."),

    // User
    INVALID_NAME_FORMAT(HttpStatus.BAD_REQUEST, "U001", "Invalid name format"),

    // Store
    STORE_REQUIRED_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "S001", "Store name and business type are required"),
    STORE_ALREADY_EXISTS(HttpStatus.CONFLICT, "S002", "User already has a registered store"),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "S003", "Store profile not found"),

    // Content/Generation Request
    GENERATION_REQUEST_REQUIRED_FIELDS_MISSING(HttpStatus.BAD_REQUEST, "C001", "Missing required fields"),
    FILE_NAME_MISMATCH(HttpStatus.BAD_REQUEST, "C002", "File name mismatch in imageConfigs"),
    INVALID_REQUEST_BODY_FORMAT(HttpStatus.BAD_REQUEST, "C003", "Invalid request body format"),
    FILE_SIZE_LIMIT_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "C004", "File size limit exceeded"),
    GENERATION_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "C005", "Request not found or already deleted"),
    INVALID_REGENERATION_REQUEST(HttpStatus.BAD_REQUEST, "C006", "Empty feedback or Invalid target platform"),
    CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "C007", "Content not found or already deleted"),

    // Generation Task
    GENERATION_ALREADY_IN_PROGRESS(HttpStatus.CONFLICT, "G001", "Generation task is already in progress or completed"),
    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "G002", "Task ID not found"),

    // Rate Limit
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "R001", "Rate limit exceeded. Please try again later."),

    // Page
    INVALID_PAGE_PARAM(HttpStatus.BAD_REQUEST, "P001", "invalid (page, size) parameter"),

    // AI / Gemini
    AI_RESPONSE_EMPTY(HttpStatus.INTERNAL_SERVER_ERROR, "AI001", "Gemini returned empty response"),
    AI_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI002", "Failed to parse AI response"),
    AI_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "AI003", "AI generation request failed. Please try again later.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
