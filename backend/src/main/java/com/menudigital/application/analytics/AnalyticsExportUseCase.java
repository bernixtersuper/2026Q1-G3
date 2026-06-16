package com.menudigital.application.analytics;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.analytics.AnalyticsExportResponse;
import com.menudigital.domain.tenant.TenantId;
import com.menudigital.infrastructure.athena.AthenaClientProducer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.services.athena.model.QueryExecutionState;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class AnalyticsExportUseCase {

  private static final int MAX_DAYS = 90;

  @Inject
  AthenaClientProducer athena;

  @Inject
  TenantContext tenantContext;

  private final Map<String, AnalyticsExportResponse> jobs = new ConcurrentHashMap<>();
  private final ExecutorService executor = Executors.newCachedThreadPool();

  public AnalyticsExportResponse startExport(int days) {
    TenantId tenantId = tenantContext.getTenantId();
    int clampedDays = Math.min(Math.max(days, 1), MAX_DAYS);
    ZoneId zone = ZoneId.systemDefault();
    LocalDate to = LocalDate.now(zone);
    LocalDate from = to.minusDays(clampedDays - 1L);

    String jobId = UUID.randomUUID().toString();
    Instant createdAt = Instant.now();
    AnalyticsExportResponse pending = new AnalyticsExportResponse(
        jobId, "RUNNING", null, createdAt, null, null
    );
    jobs.put(jobId, pending);

    executor.submit(() -> runExport(jobId, tenantId, from, to));
    return pending;
  }

  public AnalyticsExportResponse getStatus(String jobId) {
    return jobs.getOrDefault(jobId,
        new AnalyticsExportResponse(jobId, "NOT_FOUND", null, null, null, "Job no encontrado"));
  }

  private void runExport(String jobId, TenantId tenantId, LocalDate from, LocalDate to) {
    try {
      String sql = """
          SELECT "eventId", "eventType", "tenantId", "sessionId", "timestamp", "itemId", "sectionId"
          FROM events
          WHERE "tenantId" = '%s'
            AND from_iso8601_timestamp("timestamp") >= timestamp '%sT00:00:00Z'
            AND from_iso8601_timestamp("timestamp") < timestamp '%sT00:00:00Z'
          ORDER BY "timestamp"
          """.formatted(
          tenantId.value(),
          from,
          to.plusDays(1)
      );

      String queryId = athena.startQuery(sql);
      QueryExecutionState state = athena.waitForCompletion(queryId, 120);

      if (state == QueryExecutionState.SUCCEEDED) {
        String output = athena.resultOutputLocation(queryId);
        jobs.put(jobId, new AnalyticsExportResponse(
            jobId, "SUCCEEDED", output, jobs.get(jobId).createdAt(), Instant.now(), null
        ));
      } else {
        String reason = athena.failureReason(queryId);
        jobs.put(jobId, new AnalyticsExportResponse(
            jobId, state.name(), null, jobs.get(jobId).createdAt(), Instant.now(), reason
        ));
      }
    } catch (Exception e) {
      jobs.put(jobId, new AnalyticsExportResponse(
          jobId, "FAILED", null, jobs.get(jobId).createdAt(), Instant.now(), e.getMessage()
      ));
    }
  }
}
