package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.analytics.*;
import com.menudigital.domain.analytics.RealtimeAnalyticsResponse;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
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
    GetMenuInsightsUseCase getMenuInsightsUseCase;

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

    @GET
    @Path("/menu-insights")
    @Operation(summary = "Owner menu insights — bought together, menu engineering, modifiers (1–90 days)")
    public Response getMenuInsights(@QueryParam("days") @DefaultValue("30") int days) {
        return Response.ok(getMenuInsightsUseCase.execute(days)).build();
    }

    @GET
    @Path("/menu-insights/export")
    @Produces("text/csv")
    @Operation(summary = "Download menu insights as CSV (1–90 days)")
    public Response exportMenuInsights(@QueryParam("days") @DefaultValue("30") int days) {
        String csv = getMenuInsightsUseCase.exportCsv(days);
        return Response.ok(csv)
            .header("Content-Disposition", "attachment; filename=\"menu-insights-" + days + "d.csv\"")
            .build();
    }
}
