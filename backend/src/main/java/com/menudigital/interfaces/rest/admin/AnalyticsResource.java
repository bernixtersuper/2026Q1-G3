package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.analytics.*;
import com.menudigital.domain.analytics.*;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/admin/analytics")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Analytics", description = "Analytics dashboard endpoints (JWT required)")
@SecurityScheme(securitySchemeName = "jwt", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@SecurityRequirement(name = "jwt")
public class AnalyticsResource {

    @Inject
    GetAnalyticsSummaryUseCase getAnalyticsSummaryUseCase;

    @Inject
    GetAnalyticsMenuUseCase getAnalyticsMenuUseCase;

    @Inject
    GetAnalyticsOperationsUseCase getAnalyticsOperationsUseCase;

    @Inject
    GetRealtimeAnalyticsUseCase getRealtimeAnalyticsUseCase;

    @Inject
    GetAnalyticsTrendsUseCase getAnalyticsTrendsUseCase;

    @Inject
    AnalyticsExportUseCase analyticsExportUseCase;

    @GET
    @Path("/summary")
    @Operation(summary = "Business summary KPIs")
    public Response getSummary() {
        return Response.ok(getAnalyticsSummaryUseCase.execute()).build();
    }

    @GET
    @Path("/menu")
    @Operation(summary = "Menu demand — top sold and viewed")
    public Response getMenuAnalytics() {
        return Response.ok(getAnalyticsMenuUseCase.execute()).build();
    }

    @GET
    @Path("/operations")
    @Operation(summary = "Operations — heatmaps and peak hours")
    public Response getOperations() {
        return Response.ok(getAnalyticsOperationsUseCase.execute()).build();
    }

    @GET
    @Path("/realtime")
    @Operation(summary = "Realtime activity (60 min)")
    public Response getRealtimeAnalytics() {
        RealtimeAnalyticsResponse response = getRealtimeAnalyticsUseCase.execute();
        return Response.ok(response).build();
    }

    @GET
    @Path("/trends")
    @Operation(summary = "Historical trends (7–90 days)")
    public Response getTrends(@QueryParam("days") @DefaultValue("30") int days) {
        return Response.ok(getAnalyticsTrendsUseCase.execute(days)).build();
    }

    @POST
    @Path("/export")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Start async CSV export via Athena")
    public Response startExport(ExportRequest request) {
        int days = request != null && request.days() != null ? request.days() : 30;
        return Response.accepted(analyticsExportUseCase.startExport(days)).build();
    }

    @GET
    @Path("/export/{jobId}")
    @Operation(summary = "Poll async export job status")
    public Response getExportStatus(@PathParam("jobId") String jobId) {
        return Response.ok(analyticsExportUseCase.getStatus(jobId)).build();
    }

    public record ExportRequest(Integer days) {}
}
