package today_store.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AES 암호화 유틸 테스트")
class AesEncryptorTest {

    @Test
    @DisplayName("토큰 암호화와 복호화")
    void shouldEncryptAndDecryptToken() {
        // 인스타그램 access token은 AES-GCM으로 저장되므로 암호문에 원문이 노출되지 않고
        // 복호화 시 원래 토큰 값을 정확히 복원해야 한다.

        // given
        AesEncryptor aesEncryptor = new AesEncryptor("12345678901234567890123456789012");

        // when
        String encryptedToken = aesEncryptor.encrypt("instagram-access-token");
        String decryptedToken = aesEncryptor.decrypt(encryptedToken);

        // then
        assertThat(encryptedToken).isNotEqualTo("instagram-access-token");
        assertThat(decryptedToken).isEqualTo("instagram-access-token");
    }

    @Test
    @DisplayName("매번 다른 IV 사용")
    void shouldProduceDifferentCiphertextForSamePlainText() {
        // AES-GCM 암호화는 매번 새로운 IV를 사용해야 하므로
        // 같은 토큰을 반복 저장해도 동일한 암호문으로 재사용 패턴이 노출되지 않아야 한다.

        // given
        AesEncryptor aesEncryptor = new AesEncryptor("12345678901234567890123456789012");

        // when
        String first = aesEncryptor.encrypt("instagram-access-token");
        String second = aesEncryptor.encrypt("instagram-access-token");

        // then
        assertThat(first).isNotEqualTo(second);
        assertThat(aesEncryptor.decrypt(first)).isEqualTo("instagram-access-token");
        assertThat(aesEncryptor.decrypt(second)).isEqualTo("instagram-access-token");
    }

    @Test
    @DisplayName("null 입력 보존")
    void shouldReturnNullWhenInputIsNull() {
        // JPA 변환 경로에서 null 토큰 값이 전달될 수 있으므로
        // 암호화를 시도하거나 예외를 던지지 않고 null을 그대로 보존해야 한다.

        // given
        AesEncryptor aesEncryptor = new AesEncryptor("12345678901234567890123456789012");

        // when
        String encrypted = aesEncryptor.encrypt(null);
        String decrypted = aesEncryptor.decrypt(null);

        // then
        assertThat(encrypted).isNull();
        assertThat(decrypted).isNull();
    }

    @Test
    @DisplayName("잘못된 키 길이 거부")
    void shouldRejectInvalidKeyLength() {
        // AES-256은 secret key가 정확히 32자여야 하므로
        // 저장된 데이터를 복호화할 수 없는 설정으로 앱이 실행되지 않게 빠르게 실패해야 한다.

        // given

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new AesEncryptor("short-key")
        );

        // then
        assertThat(exception.getMessage()).isEqualTo("Encryption secret key must be 32 characters long for AES-256");
    }
}
