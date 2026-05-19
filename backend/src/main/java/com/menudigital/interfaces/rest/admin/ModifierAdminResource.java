package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.menu.MenuItemModifier;
import com.menudigital.domain.menu.MenuRepository;
import com.menudigital.domain.menu.ModifierType;
import com.menudigital.domain.tenant.TenantId;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Path("/api/admin/modifiers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Modifier Admin", description = "Manage product modifiers")
@SecurityRequirement(name = "jwt")
public class ModifierAdminResource {
    
    @Inject
    MenuRepository menuRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @GET
    @Path("/item/{itemId}")
    @Operation(summary = "Get all modifiers for a menu item")
    public Response getModifiersForItem(@PathParam("itemId") UUID itemId) {
        TenantId tenantId = tenantContext.getTenantId();
        
        if (!menuRepository.itemBelongsToTenant(itemId, tenantId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        
        List<ModifierResponse> modifiers = menuRepository.findModifiersByItemId(itemId)
            .stream()
            .map(this::toResponse)
            .toList();
        
        return Response.ok(modifiers).build();
    }
    
    @POST
    @Transactional
    @Operation(summary = "Create a new modifier for a menu item")
    public Response createModifier(@Valid CreateModifierRequest request) {
        TenantId tenantId = tenantContext.getTenantId();
        
        if (!menuRepository.itemBelongsToTenant(UUID.fromString(request.menuItemId()), tenantId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        
        MenuItemModifier modifier = MenuItemModifier.create(
            UUID.fromString(request.menuItemId()),
            tenantId.value(),
            request.name(),
            new BigDecimal(request.priceAdjustment()),
            ModifierType.valueOf(request.modifierType()),
            request.displayOrder()
        );
        
        menuRepository.saveModifier(modifier);
        
        return Response.status(Response.Status.CREATED)
            .entity(toResponse(modifier))
            .build();
    }
    
    @PUT
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Update a modifier")
    public Response updateModifier(
            @PathParam("id") UUID id,
            @Valid UpdateModifierRequest request) {
        
        UUID tenantUuid = tenantContext.getTenantId().value();
        
        return menuRepository.findModifierById(id)
            .filter(m -> m.getTenantId().equals(tenantUuid))
            .map(modifier -> {
                modifier.setName(request.name());
                modifier.setPriceAdjustment(new BigDecimal(request.priceAdjustment()));
                modifier.setModifierType(ModifierType.valueOf(request.modifierType()));
                modifier.setAvailable(request.available());
                modifier.setDisplayOrder(request.displayOrder());
                
                menuRepository.updateModifier(modifier);
                
                return Response.ok(toResponse(modifier)).build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    @DELETE
    @Path("/{id}")
    @Transactional
    @Operation(summary = "Delete a modifier")
    public Response deleteModifier(@PathParam("id") UUID id) {
        UUID tenantUuid = tenantContext.getTenantId().value();
        
        return menuRepository.findModifierById(id)
            .filter(m -> m.getTenantId().equals(tenantUuid))
            .map(modifier -> {
                menuRepository.deleteModifier(id);
                return Response.noContent().build();
            })
            .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }
    
    private ModifierResponse toResponse(MenuItemModifier modifier) {
        return new ModifierResponse(
            modifier.getId().toString(),
            modifier.getMenuItemId().toString(),
            modifier.getName(),
            modifier.getPriceAdjustment().toPlainString(),
            modifier.getModifierType().name(),
            modifier.isAvailable(),
            modifier.getDisplayOrder()
        );
    }
    
    public record CreateModifierRequest(
        @NotBlank String menuItemId,
        @NotBlank @Size(max = 100) String name,
        @NotNull String priceAdjustment,
        @NotBlank String modifierType,
        int displayOrder
    ) {}
    
    public record UpdateModifierRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull String priceAdjustment,
        @NotBlank String modifierType,
        boolean available,
        int displayOrder
    ) {}
    
    public record ModifierResponse(
        String id,
        String menuItemId,
        String name,
        String priceAdjustment,
        String modifierType,
        boolean available,
        int displayOrder
    ) {}
}
