package com.menudigital.interfaces.rest.public_;

import com.menudigital.application.analytics.RecordInteractionUseCase;
import com.menudigital.application.analytics.dto.RecordEventCommand;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.quarkus.logging.Log;

@Path("/api/menu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Public Menu", description = "Public endpoints for viewing menus (no auth required)")
public class PublicEventResource {
    
    @Inject
    RecordInteractionUseCase recordInteractionUseCase;
    
    @POST
    @Path("/{slug}/events")
    @Operation(summary = "Record analytics event", description = "Records a menu interaction event (view, item click, filter use)")
    @APIResponse(responseCode = "204", description = "Event recorded")
    @APIResponse(responseCode = "404", description = "Restaurant not found")
    @APIResponse(responseCode = "500", description = "Failed to save event to DynamoDB")
    public Response recordEvent(
            @Parameter(description = "Restaurant URL slug") @PathParam("slug") String slug, 
            @Valid RecordEventCommand command) {
        try {
            recordInteractionUseCase.execute(slug, command);
            Log.infof("Event recorded successfully - slug: %s, eventType: %s", slug, command.eventType());
            return Response.noContent().build();
        } catch (RecordInteractionUseCase.RestaurantNotFoundException e) {
            Log.warnf("Restaurant not found - slug: %s", slug);
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                .build();
        } catch (Exception e) {
            Log.errorf("Failed to record event - slug: %s, error: %s", slug, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("INTERNAL_ERROR", "Failed to save event: " + e.getMessage()))
                .build();
        }
    }
    
    public record ErrorResponse(String code, String message) {}
}
