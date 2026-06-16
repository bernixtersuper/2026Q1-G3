package com.menudigital.infrastructure.kinesis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kinesis.KinesisClient;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
public class KinesisClientProducer {

    @ConfigProperty(name = "aws.kinesis.region", defaultValue = "us-east-1")
    String region;

    @ConfigProperty(name = "aws.dynamodb.endpoint-override")
    Optional<String> endpointOverride;

    @ConfigProperty(name = "aws.dynamodb.access-key")
    Optional<String> accessKey;

    @ConfigProperty(name = "aws.dynamodb.secret-key")
    Optional<String> secretKey;

    @Produces
    @ApplicationScoped
    public KinesisClient kinesisClient() {
        var builder = KinesisClient.builder()
            .region(Region.of(region));

        endpointOverride.filter(e -> !e.isBlank())
            .ifPresent(e -> builder.endpointOverride(URI.create(e)));

        String key = accessKey.orElse("");
        String secret = secretKey.orElse("");
        if (!key.isBlank() && !secret.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(key, secret)
            ));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }
}
