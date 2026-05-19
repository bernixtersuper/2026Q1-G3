package com.menudigital.infrastructure.dynamo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.Optional;

@ApplicationScoped
public class DynamoClientProducer {
    
    @ConfigProperty(name = "aws.dynamodb.region", defaultValue = "us-east-1")
    String region;
    
    @ConfigProperty(name = "aws.dynamodb.endpoint-override")
    Optional<String> endpointOverride;
    
    @ConfigProperty(name = "aws.dynamodb.access-key")
    Optional<String> accessKey;
    
    @ConfigProperty(name = "aws.dynamodb.secret-key")
    Optional<String> secretKey;
    
    @Produces
    @ApplicationScoped
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder()
            .region(Region.of(region));
        
        String endpoint = endpointOverride.orElse("");
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        
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
