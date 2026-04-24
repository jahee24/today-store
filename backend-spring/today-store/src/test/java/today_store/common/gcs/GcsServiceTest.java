package today_store.common.gcs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("GCS 서비스 테스트")
class GcsServiceTest {

    @Mock
    private Storage storage;

    private GcsService gcsService;

    @BeforeEach
    void setUp() {
        gcsService = new GcsService(storage);
        ReflectionTestUtils.setField(gcsService, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("이미지 업로드 성공")
    void shouldUploadFileToGcs() throws Exception {
        // 업로드 요청이 오면 bucket, object name, content type을 반영해 storage.create를 호출해야 한다.

        // given
        MockMultipartFile file = new MockMultipartFile(
                "images",
                "sample.png",
                "image/png",
                "image-data".getBytes(StandardCharsets.UTF_8)
        );
        given(storage.create(any(BlobInfo.class), any(byte[].class))).willReturn(null);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // when
        String objectName = gcsService.uploadFile(file, "requests");

        // then
        assertThat(objectName).matches("^" + datePath + "/requests/[0-9a-f\\-]+_sample\\.png$");

        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        then(storage).should().create(blobInfoCaptor.capture(), bytesCaptor.capture());
        assertThat(blobInfoCaptor.getValue().getBucket()).isEqualTo("test-bucket");
        assertThat(blobInfoCaptor.getValue().getName()).isEqualTo(objectName);
        assertThat(blobInfoCaptor.getValue().getContentType()).isEqualTo("image/png");
        assertThat(bytesCaptor.getValue()).containsExactly("image-data".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("이미지 업로드 실패")
    void shouldThrowWhenUploadFails() throws Exception {
        // 파일 바이트를 읽지 못하면 GCS 업로드 실패 런타임 예외로 감싸야 한다.

        // given
        org.springframework.web.multipart.MultipartFile file = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        given(file.getOriginalFilename()).willReturn("sample.png");
        given(file.getContentType()).willReturn("image/png");
        given(file.getBytes()).willThrow(new IOException("read failed"));

        // when
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> gcsService.uploadFile(file, "requests")
        );

        // then
        assertThat(exception.getMessage()).isEqualTo("GCS upload failed");
        assertThat(exception.getCause()).isInstanceOf(IOException.class);
    }

    @Test
    @DisplayName("Signed URL 생성 성공")
    void shouldGenerateSignedUrl() throws Exception {
        // objectName이 있으면 storage.signUrl로 signed URL 문자열을 생성해야 한다.

        // given
        URL signedUrl = new URL("https://signed.example.com/image");
        given(storage.signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES), any(Storage.SignUrlOption[].class)))
                .willReturn(signedUrl);

        // when
        String result = gcsService.generateSignedUrl("2026/03/30/requests/sample.png");

        // then
        assertThat(result).isEqualTo("https://signed.example.com/image");

        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        then(storage).should().signUrl(
                blobInfoCaptor.capture(),
                eq(15L),
                eq(TimeUnit.MINUTES),
                any(Storage.SignUrlOption[].class)
        );
        assertThat(blobInfoCaptor.getValue().getBlobId().getBucket()).isEqualTo("test-bucket");
        assertThat(blobInfoCaptor.getValue().getBlobId().getName()).isEqualTo("2026/03/30/requests/sample.png");
    }

    @Test
    @DisplayName("Signed URL 생성 입력값 비어 있음")
    void shouldReturnNullWhenObjectNameIsBlankForSignedUrl() {
        // objectName이 null 또는 빈 문자열이면 signed URL 생성 없이 null을 반환해야 한다.

        // given

        // when
        String nullResult = gcsService.generateSignedUrl(null);
        String blankResult = gcsService.generateSignedUrl("");

        // then
        assertThat(nullResult).isNull();
        assertThat(blankResult).isNull();
        then(storage).should(never()).signUrl(any(BlobInfo.class), eq(15L), eq(TimeUnit.MINUTES), any(Storage.SignUrlOption[].class));
    }

    @Test
    @DisplayName("이미지 복사 성공")
    void shouldCopyFileInGcs() {
        // copy 요청이 들어오면 source와 target이 올바른 CopyRequest로 구성되어야 한다.

        // given
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        // when
        String copiedObjectName = gcsService.copyFile("2026/04/14/requests/sample.png", "contents");

        // then
        assertThat(copiedObjectName).matches("^" + datePath + "/contents/[0-9a-f\\-]+_sample\\.png$");

        ArgumentCaptor<Storage.CopyRequest> copyRequestCaptor = ArgumentCaptor.forClass(Storage.CopyRequest.class);
        then(storage).should().copy(copyRequestCaptor.capture());
        assertThat(copyRequestCaptor.getValue().getSource().getBucket()).isEqualTo("test-bucket");
        assertThat(copyRequestCaptor.getValue().getSource().getName()).isEqualTo("2026/04/14/requests/sample.png");
        assertThat(copyRequestCaptor.getValue().getTarget().getBlobId().getBucket()).isEqualTo("test-bucket");
        assertThat(copyRequestCaptor.getValue().getTarget().getBlobId().getName()).isEqualTo(copiedObjectName);
    }

    @Test
    @DisplayName("이미지 복사 입력값 비어 있음")
    void shouldReturnNullWhenCopySourceIsBlank() {
        // source object가 비어 있으면 copy를 시도하지 않고 null을 반환해야 한다.

        // when
        String nullResult = gcsService.copyFile(null, "contents");
        String blankResult = gcsService.copyFile("", "contents");

        // then
        assertThat(nullResult).isNull();
        assertThat(blankResult).isNull();
        then(storage).should(never()).copy(any(Storage.CopyRequest.class));
    }

    @Test
    @DisplayName("이미지 복사 실패")
    void shouldThrowWhenCopyFails() {
        // storage.copy 실패는 표준화된 copy 예외로 감싸야 한다.

        // given
        given(storage.copy(any(Storage.CopyRequest.class))).willThrow(new RuntimeException("copy failed"));

        // when
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> gcsService.copyFile("2026/04/14/requests/sample.png", "contents")
        );

        // then
        assertThat(exception.getMessage()).isEqualTo("GCS copy failed");
        assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("이미지 삭제 성공")
    void shouldDeleteSanitizedObjectName() {
        // objectName이 있으면 sanitize 후 storage.delete를 호출해야 한다.

        // given
        given(storage.delete(any(BlobId.class))).willReturn(true);

        // when
        gcsService.deleteFile("folder/\nfile.png");

        // then
        ArgumentCaptor<BlobId> blobIdCaptor = ArgumentCaptor.forClass(BlobId.class);
        then(storage).should().delete(blobIdCaptor.capture());
        assertThat(blobIdCaptor.getValue().getBucket()).isEqualTo("test-bucket");
        assertThat(blobIdCaptor.getValue().getName()).isEqualTo("folder/ file.png");
    }

    @Test
    @DisplayName("이미지 삭제 입력값 비어 있음")
    void shouldSkipDeleteWhenObjectNameIsBlank() {
        // objectName이 null 또는 빈 문자열이면 삭제를 시도하지 않아야 한다.

        // given

        // when
        gcsService.deleteFile(null);
        gcsService.deleteFile("");

        // then
        then(storage).should(never()).delete(any(BlobId.class));
    }
}
