package com.menudigital.infrastructure.athena;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.util.List;

@ApplicationScoped
public class AthenaClientProducer {

    @ConfigProperty(name = "aws.athena.workgroup", defaultValue = "menuqr-analytics")
    String workgroup;

    @ConfigProperty(name = "aws.athena.database", defaultValue = "menuqr_analytics")
    String database;

    @ConfigProperty(name = "aws.athena.results-bucket", defaultValue = "")
    String resultsBucket;

    @ConfigProperty(name = "aws.athena.region", defaultValue = "us-east-1")
    String region;

    private AthenaClient client;

    AthenaClient client() {
        if (client == null) {
            client = AthenaClient.builder()
                .region(Region.of(region))
                .build();
        }
        return client;
    }

    public String startQuery(String sql) {
        String outputLocation = resultsBucket.isBlank()
            ? null
            : "s3://" + resultsBucket + "/athena-results/";

        StartQueryExecutionRequest.Builder builder = StartQueryExecutionRequest.builder()
            .queryString(sql)
            .workGroup(workgroup)
            .queryExecutionContext(QueryExecutionContext.builder().database(database).build());

        if (outputLocation != null) {
            builder.resultConfiguration(
                ResultConfiguration.builder().outputLocation(outputLocation).build()
            );
        }

        return client().startQueryExecution(builder.build()).queryExecutionId();
    }

    public QueryExecutionState waitForCompletion(String queryExecutionId, int maxAttempts) {
        for (int i = 0; i < maxAttempts; i++) {
            GetQueryExecutionResponse response = client().getQueryExecution(
                GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build()
            );
            QueryExecutionState state = response.queryExecution().status().state();
            if (state == QueryExecutionState.SUCCEEDED
                || state == QueryExecutionState.FAILED
                || state == QueryExecutionState.CANCELLED) {
                return state;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return QueryExecutionState.FAILED;
            }
        }
        return QueryExecutionState.RUNNING;
    }

    public String resultOutputLocation(String queryExecutionId) {
        GetQueryExecutionResponse response = client().getQueryExecution(
            GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build()
        );
        return response.queryExecution().resultConfiguration().outputLocation();
    }

    public String failureReason(String queryExecutionId) {
        GetQueryExecutionResponse response = client().getQueryExecution(
            GetQueryExecutionRequest.builder().queryExecutionId(queryExecutionId).build()
        );
        return response.queryExecution().status().stateChangeReason();
    }

    public String database() {
        return database;
    }
}
