package com.menudigital.infrastructure.storage;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.Optional;

/**
 * Cliente S3 con la misma configuración que el resto de la app (MinIO local vs AWS).
 */
@ApplicationScoped
public class S3ClientFactory {

    @ConfigProperty(name = "aws.s3.region", defaultValue = "us-east-1")
    String region;

    @ConfigProperty(name = "aws.s3.endpoint-override")
    Optional<String> endpointOverride;

    @ConfigProperty(name = "aws.s3.access-key", defaultValue = "local")
    String accessKey;

    @ConfigProperty(name = "aws.s3.secret-key", defaultValue = "local")
    String secretKey;

    public S3Client createClient() {
        var builder = S3Client.builder()
            .region(Region.of(region));

        if (endpointOverride.isPresent() && !endpointOverride.get().isBlank()) {
            builder.endpointOverride(URI.create(endpointOverride.get()));
            builder.forcePathStyle(true);
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
