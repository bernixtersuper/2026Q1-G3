package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.analytics.GetMenuAnalyticsUseCase;
import com.menudigital.application.analytics.GetRealtimeAnalyticsUseCase;
import com.menudigital.domain.analytics.AnalyticsDashboardResponse;
import com.menudigital.domain.analytics.RealtimeAnalyticsResponse;
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
    GetMenuAnalyticsUseCase getMenuAnalyticsUseCase;
    
    @Inject
    GetRealtimeAnalyticsUseCase getRealtimeAnalyticsUseCase;
    
    @GET
    @Operation(summary = "Get analytics dashboard", description = "Returns full analytics data for the last 30 days")
    @APIResponse(responseCode = "200", description = "Analytics data")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response getAnalytics() {
        AnalyticsDashboardResponse response = getMenuAnalyticsUseCase.execute();
        return Response.ok(response).build();
    }
    
    @GET
    @Path("/realtime")
    @Operation(summary = "Get realtime analytics", description = "Returns activity data for the last 60 minutes in 5-minute buckets")
    @APIResponse(responseCode = "200", description = "Realtime analytics data")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    public Response getRealtimeAnalytics() {
        RealtimeAnalyticsResponse response = getRealtimeAnalyticsUseCase.execute();
        return Response.ok(response).build();
    }
}
