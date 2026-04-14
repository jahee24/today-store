package today_store.common.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import today_store.common.util.AesEncryptor;

@Component
@Converter
@RequiredArgsConstructor
public class TokenConverter implements AttributeConverter<String, String> {

    private final AesEncryptor aesEncryptor;

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        return aesEncryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return aesEncryptor.decrypt(dbData);
    }
}
