package today_store.common.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class GcsService {

    private final Storage storage;

    @Value("${app.gcs.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String folder) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        String uuid = UUID.randomUUID().toString();

        String objectName = String.format("%s/%s/%s_%s",
                datePath, folder, uuid, file.getOriginalFilename());

        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                    .setContentType(file.getContentType())
                    .build();
            storage.create(blobInfo, file.getBytes());
            log.info("Successfully uploaded file to GCS. ID: {}", uuid);
            return objectName;
        } catch (IOException e) {
            log.error("Failed to upload file to GCS: {}", e.getMessage());
            throw new RuntimeException("GCS upload failed", e);
        }
    }

    public String uploadFile(byte[] bytes, String contentType, String folder, String filename) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString();
        String objectName = String.format("%s/%s/%s_%s",
                datePath, folder, uuid, filename);

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, objectName)
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, bytes);
        log.info("Successfully uploaded byte array to GCS. ID: {}", uuid);
        return objectName;
    }

    public String generateSignedUrl(String objectName) {
        return generateSignedUrl(objectName, 15, TimeUnit.MINUTES);
    }

    public String generateSignedUrl(String objectName, long duration, TimeUnit unit) {
        if (objectName == null || objectName.isEmpty()) {
            return null;
        }

        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();

        URL signedUrl = storage.signUrl(blobInfo, duration, unit, Storage.SignUrlOption.withV4Signature());
        return signedUrl.toString();
    }

    public void deleteFile(String objectName) {
        if (objectName == null || objectName.isEmpty()) {
            return;
        }

        boolean deleted = storage.delete(BlobId.of(bucketName, sanitize(objectName)));
        if (!deleted) {
            log.warn("File not found in GCS: {}", objectName);
        }
    }

    public String copyFile(String sourceObjectName, String targetFolder) {
        if (sourceObjectName == null || sourceObjectName.isEmpty()) {
            return null;
        }

        String fileName = sourceObjectName.substring(sourceObjectName.lastIndexOf("/") + 1);
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uuid = UUID.randomUUID().toString();
        String targetObjectName = String.format("%s/%s/%s_%s",
                datePath, targetFolder, uuid, fileName);

        try {
            Storage.CopyRequest request = Storage.CopyRequest.newBuilder()
                    .setSource(BlobId.of(bucketName, sourceObjectName))
                    .setTarget(BlobId.of(bucketName, targetObjectName))
                    .build();
            storage.copy(request);
            log.info("Successfully copied file from {} to {}", sourceObjectName, targetObjectName);
            return targetObjectName;
        } catch (Exception e) {
            log.error("Failed to copy file in GCS: {}", e.getMessage());
            throw new RuntimeException("GCS copy failed", e);
        }
    }

    private String sanitize(String input) {
        return (input == null) ? "" : input.replaceAll("[\r\n]", " ");
    }
}