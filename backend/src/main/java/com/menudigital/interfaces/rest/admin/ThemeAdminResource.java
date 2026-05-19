package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.tenant.Restaurant;
import com.menudigital.domain.tenant.RestaurantRepository;
import com.menudigital.domain.tenant.RestaurantTheme;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/admin/theme")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Theme Customization", description = "Customize restaurant theme and branding")
@SecurityRequirement(name = "jwt")
public class ThemeAdminResource {
    
    @Inject
    RestaurantRepository restaurantRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @GET
    @Operation(summary = "Get current theme")
    public Response getTheme() {
        return restaurantRepository.findById(tenantContext.getTenantId())
            .map(r -> Response.ok(toResponse(r.getTheme())).build())
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @PUT
    @Transactional
    @Operation(summary = "Update theme")
    public Response updateTheme(UpdateThemeRequest request) {
        return restaurantRepository.findById(tenantContext.getTenantId())
            .map(restaurant -> {
                RestaurantTheme newTheme = new RestaurantTheme(
                    request.primaryColor() != null ? request.primaryColor() : restaurant.getTheme().primaryColor(),
                    request.secondaryColor() != null ? request.secondaryColor() : restaurant.getTheme().secondaryColor(),
                    request.accentColor() != null ? request.accentColor() : restaurant.getTheme().accentColor(),
                    request.backgroundColor() != null ? request.backgroundColor() : restaurant.getTheme().backgroundColor(),
                    request.textColor() != null ? request.textColor() : restaurant.getTheme().textColor(),
                    request.cardBackground() != null ? request.cardBackground() : restaurant.getTheme().cardBackground(),
                    request.gradientStart() != null ? request.gradientStart() : restaurant.getTheme().gradientStart(),
                    request.gradientEnd() != null ? request.gradientEnd() : restaurant.getTheme().gradientEnd(),
                    request.gradientDirection() != null ? request.gradientDirection() : restaurant.getTheme().gradientDirection(),
                    request.fontFamily() != null ? request.fontFamily() : restaurant.getTheme().fontFamily(),
                    request.borderRadius() != null ? request.borderRadius() : restaurant.getTheme().borderRadius(),
                    request.logoUrl() != null ? request.logoUrl() : restaurant.getTheme().logoUrl(),
                    request.bannerUrl() != null ? request.bannerUrl() : restaurant.getTheme().bannerUrl(),
                    request.showGradientHeader() != null ? request.showGradientHeader() : restaurant.getTheme().showGradientHeader()
                );
                restaurant.setTheme(newTheme);
                restaurantRepository.update(restaurant);
                return Response.ok(toResponse(newTheme)).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @POST
    @Path("/reset")
    @Transactional
    @Operation(summary = "Reset theme to defaults")
    public Response resetTheme() {
        return restaurantRepository.findById(tenantContext.getTenantId())
            .map(restaurant -> {
                restaurant.setTheme(RestaurantTheme.defaultTheme());
                restaurantRepository.update(restaurant);
                return Response.ok(toResponse(restaurant.getTheme())).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    private ThemeResponse toResponse(RestaurantTheme theme) {
        return new ThemeResponse(
            theme.primaryColor(),
            theme.secondaryColor(),
            theme.accentColor(),
            theme.backgroundColor(),
            theme.textColor(),
            theme.cardBackground(),
            theme.gradientStart(),
            theme.gradientEnd(),
            theme.gradientDirection(),
            theme.fontFamily(),
            theme.borderRadius(),
            theme.logoUrl(),
            theme.bannerUrl(),
            theme.showGradientHeader()
        );
    }
    
    public record UpdateThemeRequest(
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String backgroundColor,
        String textColor,
        String cardBackground,
        String gradientStart,
        String gradientEnd,
        String gradientDirection,
        String fontFamily,
        String borderRadius,
        String logoUrl,
        String bannerUrl,
        Boolean showGradientHeader
    ) {}
    
    public record ThemeResponse(
        String primaryColor,
        String secondaryColor,
        String accentColor,
        String backgroundColor,
        String textColor,
        String cardBackground,
        String gradientStart,
        String gradientEnd,
        String gradientDirection,
        String fontFamily,
        String borderRadius,
        String logoUrl,
        String bannerUrl,
        boolean showGradientHeader
    ) {}
}
