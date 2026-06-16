package com.menudigital.infrastructure.storage;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@ApplicationScoped
public class S3ImageStorageService {

    @Inject
    S3ClientFactory s3ClientFactory;

    @Inject
    MenuImageUrls menuImageUrls;

    @ConfigProperty(name = "aws.s3.bucket", defaultValue = "menudigital-images")
    String bucketName;

    @ConfigProperty(name = "aws.s3.presign-ttl-seconds", defaultValue = "3600")
    int presignTtlSeconds;

    private volatile S3Presigner presigner;

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

    /** URL prefirmada GET de corta duración; vacío si el valor no es una clave válida. */
    public String toPresignedUrl(String storedOrKey) {
        String key = menuImageUrls.normalizeForStorage(storedOrKey);
        if (key.isEmpty()) {
            return "";
        }
        var presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(Math.max(60, presignTtlSeconds)))
            .getObjectRequest(b -> b.bucket(bucketName).key(key))
            .build();
        return presigner().presignGetObject(presignRequest).url().toExternalForm();
    }

    private S3Presigner presigner() {
        S3Presigner local = presigner;
        if (local == null) {
            synchronized (this) {
                local = presigner;
                if (local == null) {
                    presigner = local = s3ClientFactory.createPresigner();
                }
            }
        }
        return local;
    }

    @PreDestroy
    void closePresigner() {
        S3Presigner local = presigner;
        if (local != null) {
            local.close();
        }
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
