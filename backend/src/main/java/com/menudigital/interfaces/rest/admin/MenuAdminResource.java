package com.menudigital.interfaces.rest.admin;

import com.menudigital.application.menu.*;
import com.menudigital.application.menu.dto.MenuDTOs.*;
import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.menu.Menu;
import com.menudigital.domain.menu.MenuItem;
import com.menudigital.domain.menu.MenuSection;
import com.menudigital.domain.tenant.Restaurant;
import com.menudigital.domain.tenant.RestaurantRepository;
import com.menudigital.infrastructure.storage.MenuImageUrls;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/admin/menu")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Menu Management", description = "CRUD operations for menu sections and items (JWT required)")
@SecurityRequirement(name = "jwt")
public class MenuAdminResource {
    
    @Inject
    MenuImageUrls menuImageUrls;

    @Inject
    GetAdminMenuUseCase getAdminMenuUseCase;
    
    @Inject
    CreateMenuSectionUseCase createMenuSectionUseCase;
    
    @Inject
    UpdateMenuSectionUseCase updateMenuSectionUseCase;
    
    @Inject
    DeleteMenuSectionUseCase deleteMenuSectionUseCase;
    
    @Inject
    AddMenuItemUseCase addMenuItemUseCase;
    
    @Inject
    UpdateMenuItemUseCase updateMenuItemUseCase;
    
    @Inject
    DeleteMenuItemUseCase deleteMenuItemUseCase;
    
    @Inject
    ToggleItemAvailabilityUseCase toggleItemAvailabilityUseCase;
    
    @Inject
    RestaurantRepository restaurantRepository;
    
    @Inject
    TenantContext tenantContext;
    
    @GET
    public Response getMenu() {
        Menu menu = getAdminMenuUseCase.execute();
        return Response.ok(toMenuResponse(menu)).build();
    }
    
    @PUT
    @Path("/restaurant")
    public Response updateRestaurant(@Valid UpdateRestaurantCommand command) {
        Restaurant restaurant = restaurantRepository.findById(tenantContext.getTenantId())
            .orElseThrow(() -> new NotFoundException("Restaurant not found"));
        
        if (!restaurant.getSlug().equals(command.slug()) && 
            restaurantRepository.existsBySlug(command.slug())) {
            return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("SLUG_EXISTS", "Slug already in use"))
                .build();
        }
        
        restaurant.setName(command.name());
        restaurant.setSlug(command.slug());
        restaurantRepository.update(restaurant);
        
        return Response.ok().build();
    }
    
    @POST
    @Path("/sections")
    public Response createSection(@Valid CreateSectionCommand command) {
        MenuSection section = createMenuSectionUseCase.execute(command);
        return Response.status(Response.Status.CREATED)
            .entity(toSectionResponse(section))
            .build();
    }
    
    @PUT
    @Path("/sections/{id}")
    public Response updateSection(@PathParam("id") UUID id, @Valid UpdateSectionCommand command) {
        try {
            updateMenuSectionUseCase.execute(id, command);
            return Response.ok().build();
        } catch (UpdateMenuSectionUseCase.SectionNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @DELETE
    @Path("/sections/{id}")
    public Response deleteSection(@PathParam("id") UUID id) {
        try {
            deleteMenuSectionUseCase.execute(id);
            return Response.noContent().build();
        } catch (DeleteMenuSectionUseCase.SectionNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @POST
    @Path("/items")
    public Response createItem(@Valid CreateItemCommand command) {
        try {
            MenuItem item = addMenuItemUseCase.execute(command);
            return Response.status(Response.Status.CREATED)
                .entity(toItemResponse(item))
                .build();
        } catch (AddMenuItemUseCase.SectionNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @PUT
    @Path("/items/{id}")
    public Response updateItem(@PathParam("id") UUID id, @Valid UpdateItemCommand command) {
        try {
            updateMenuItemUseCase.execute(id, command);
            return Response.ok().build();
        } catch (UpdateMenuItemUseCase.ItemNotFoundException | UpdateMenuItemUseCase.SectionNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @DELETE
    @Path("/items/{id}")
    public Response deleteItem(@PathParam("id") UUID id) {
        try {
            deleteMenuItemUseCase.execute(id);
            return Response.noContent().build();
        } catch (DeleteMenuItemUseCase.ItemNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    @PATCH
    @Path("/items/{id}/availability")
    public Response toggleAvailability(@PathParam("id") UUID id, @Valid AvailabilityCommand command) {
        try {
            toggleItemAvailabilityUseCase.execute(id, command.available());
            return Response.ok().build();
        } catch (ToggleItemAvailabilityUseCase.ItemNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", e.getMessage()))
                .build();
        }
    }
    
    private MenuResponse toMenuResponse(Menu menu) {
        return new MenuResponse(
            menu.getTenantId() != null ? menu.getTenantId().toString() : null,
            menu.getRestaurantName(),
            menu.getSlug(),
            menu.getSortedSections().stream()
                .map(this::toSectionResponse)
                .toList()
        );
    }
    
    private SectionResponse toSectionResponse(MenuSection section) {
        return new SectionResponse(
            section.getId().toString(),
            section.getName(),
            section.getDisplayOrder(),
            section.getItems().stream()
                .map(this::toItemResponse)
                .toList()
        );
    }
    
    private ItemResponse toItemResponse(MenuItem item) {
        return new ItemResponse(
            item.getId().toString(),
            item.getSectionId().toString(),
            item.getName(),
            item.getDescription(),
            item.getPrice().toPlainString(),
            menuImageUrls.toApiPath(item.getImageUrl()),
            item.isAvailable(),
            item.getDietaryTags(),
            item.getDisplayOrder()
        );
    }
    
    public record ErrorResponse(String code, String message) {}
}
