package com.menudigital.interfaces.rest;

import com.menudigital.application.dto.AnalyticsDashboardDTO;
import com.menudigital.application.service.AnalyticsService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Path("/api/v1/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AnalyticsController {

    @Inject
    AnalyticsService analyticsService;

    @GET
    @Path("/dashboard")
    public Response getDashboardMetrics(
            @QueryParam("tenantId") String tenantId,
            @QueryParam("days") @DefaultValue("30") int days) {

        if (tenantId == null || tenantId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("tenantId is required").build();
        }

        Instant to = Instant.now();
        Instant from = to.minus(days, ChronoUnit.DAYS);

        AnalyticsDashboardDTO dto = analyticsService.getDashboardAnalytics(tenantId, from, to);
        return Response.ok(dto).build();
    }
}
