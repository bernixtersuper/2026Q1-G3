package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.analytics.DemoSeedUseCase;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;

/**
 * Hidden demo utility: seeds the authenticated tenant with a full dataset so dashboards
 * look populated during a presentation. Disabled unless {@code menudigital.demo.seed-enabled}
 * is true; when disabled it 404s so the endpoint is effectively invisible. Not linked from
 * any UI. Seeds ONLY the caller's own tenant (resolved from the JWT).
 */
@Path("/api/admin/demo")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class DemoResource {

    @Inject
    DemoSeedUseCase demoSeedUseCase;

    @ConfigProperty(name = "menudigital.demo.seed-enabled", defaultValue = "false")
    boolean seedEnabled;

    @POST
    @Path("/seed")
    @Operation(summary = "Seed the current tenant with demo data (hidden, flag-gated)")
    public Response seed(@QueryParam("days") @DefaultValue("30") Integer days) {
        if (!seedEnabled) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(demoSeedUseCase.execute(days)).build();
    }
}
