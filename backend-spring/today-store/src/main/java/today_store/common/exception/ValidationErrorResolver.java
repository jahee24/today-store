package today_store.common.exception;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ValidationErrorResolver {
    private final Map<String, ErrorCode> errorMapping = new HashMap<>();

    public ValidationErrorResolver() {
        // DTO ObjectName <-> ErrorCode 매핑
        errorMapping.put("createStoreRequest", ErrorCode.STORE_REQUIRED_FIELDS_MISSING);
        errorMapping.put("updateUserProfileRequest", ErrorCode.INVALID_NAME_FORMAT);
        errorMapping.put("createGenerationRequest", ErrorCode.GENERATION_REQUEST_REQUIRED_FIELDS_MISSING);
    }

    public ErrorCode resolve(String objectName) {
        return errorMapping.getOrDefault(objectName, ErrorCode.MISSING_REQUIRED_FIELDS);
    }
}
