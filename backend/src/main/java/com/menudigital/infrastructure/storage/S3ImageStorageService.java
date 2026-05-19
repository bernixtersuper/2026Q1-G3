package com.menudigital.infrastructure.storage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class S3ImageStorageService {

    @Inject
    S3ClientFactory s3ClientFactory;

    @Inject
    MenuImageUrls menuImageUrls;

    @ConfigProperty(name = "aws.s3.bucket", defaultValue = "menudigital-images")
    String bucketName;

    /** Sube el objeto y devuelve la clave S3 (p. ej. {@code menus/{tenantId}/{uuid}.jpg}). */
    public String upload(InputStream inputStream, String contentType, long contentLength, String tenantId) {
        String key = "menus/" + tenantId + "/" + UUID.randomUUID() + getExtension(contentType);

        S3Client s3Client = s3ClientFactory.createClient();
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
            return key;
        } finally {
            s3Client.close();
        }
    }

    public Optional<StoredMenuImage> open(String objectKey) {
        if (!menuImageUrls.isValidObjectKey(objectKey)) {
            return Optional.empty();
        }
        S3Client s3Client = s3ClientFactory.createClient();
        try {
            var response = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(objectKey).build()
            );
            String contentType = response.response().contentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = contentTypeFromKey(objectKey);
            }
            return Optional.of(new StoredMenuImage(
                response,
                response.response().contentLength() != null ? response.response().contentLength() : -1L,
                contentType
            ));
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private static String contentTypeFromKey(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    private static String getExtension(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
