package com.menudigital.interfaces.rest.auth;

import com.menudigital.application.tenant.RegisterRestaurantUseCase;
import com.menudigital.application.tenant.dto.RegisterRestaurantCommand;
import com.menudigital.application.tenant.dto.RegisterRestaurantRequest;
import com.menudigital.application.tenant.dto.RegisterRestaurantResponse;
import com.menudigital.infrastructure.auth.CognitoTokenVerifier;
import com.menudigital.infrastructure.auth.CognitoTokenVerifier.AuthenticationException;
import com.menudigital.infrastructure.auth.CognitoTokenVerifier.VerifiedIdToken;
import com.menudigital.infrastructure.persistence.UserRepositoryImpl;
import com.menudigital.infrastructure.persistence.entity.RestaurantEntity;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Authentication", description = "Cognito-backed session bootstrap")
public class AuthResource {

    @Inject
    RegisterRestaurantUseCase registerRestaurantUseCase;

    @Inject
    UserRepositoryImpl userRepository;

    @Inject
    CognitoTokenVerifier cognitoTokenVerifier;

    @POST
    @Path("/register")
    @Operation(summary = "Register a new restaurant",
        description = "Requires a verified Cognito ID token in the Authorization header. Email and Cognito sub are taken from the verified token.")
    @APIResponse(responseCode = "200", description = "Registration successful",
        content = @Content(schema = @Schema(implementation = RegisterRestaurantResponse.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid Cognito token")
    @APIResponse(responseCode = "409", description = "Slug or email already exists")
    public Response register(@Valid RegisterRestaurantRequest request,
                             @HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        VerifiedIdToken verified;
        try {
            verified = cognitoTokenVerifier.verifyIdTokenHeader(authorization);
        } catch (AuthenticationException e) {
            return unauthorized(e.getMessage());
        }

        try {
            var command = new RegisterRestaurantCommand(
                request.restaurantName(),
                request.slug(),
                verified.email(),
                verified.sub()
            );
            RegisterRestaurantResponse response = registerRestaurantUseCase.execute(command);
            return Response.ok(response).build();
        } catch (RegisterRestaurantUseCase.SlugAlreadyExistsException e) {
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("SLUG_EXISTS", e.getMessage()))
                .build();
        } catch (RegisterRestaurantUseCase.EmailAlreadyExistsException e) {
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("EMAIL_EXISTS", e.getMessage()))
                .build();
        } catch (Exception e) {
            return Response.serverError()
                .entity(new ErrorResponse("REGISTER_FAILED", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()))
                .build();
        }
    }

    @POST
    @Path("/session")
    @Operation(summary = "Bootstrap an application session for a verified Cognito identity",
        description = "Requires a verified Cognito ID token in the Authorization header. Returns the tenant metadata so the SPA can render its shell. Subsequent calls authorize with the Cognito access token directly.")
    @APIResponse(responseCode = "200", description = "Session bootstrap successful",
        content = @Content(schema = @Schema(implementation = SessionResponse.class)))
    @APIResponse(responseCode = "401", description = "Missing or invalid Cognito token, or no account exists for this identity")
    public Response session(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        VerifiedIdToken verified;
        try {
            verified = cognitoTokenVerifier.verifyIdTokenHeader(authorization);
        } catch (AuthenticationException e) {
            return unauthorized(e.getMessage());
        }

        var userOpt = userRepository.findByCognitoSubOrEmail(verified.sub(), verified.email());
        if (userOpt.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("UNKNOWN_USER", "No restaurant account exists for this identity"))
                .build();
        }

        var user = userOpt.get();
        userRepository.linkCognitoSub(user.id, verified.sub());

        var restaurant = (RestaurantEntity) RestaurantEntity.findById(user.restaurantId);
        if (restaurant == null) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("NO_RESTAURANT", "User has no associated restaurant"))
                .build();
        }

        return Response.ok(new SessionResponse(
            restaurant.id.toString(),
            restaurant.name
        )).build();
    }

    private Response unauthorized(String message) {
        return Response.status(Response.Status.UNAUTHORIZED)
            .entity(new ErrorResponse("INVALID_TOKEN", message))
            .build();
    }

    public record SessionResponse(String tenantId, String restaurantName) {}
    public record ErrorResponse(String code, String message) {}
}
