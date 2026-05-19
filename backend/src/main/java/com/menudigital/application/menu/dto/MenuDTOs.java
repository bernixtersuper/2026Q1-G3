package com.menudigital.application.menu.dto;

import com.menudigital.domain.menu.DietaryTag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class MenuDTOs {
    
    private MenuDTOs() {}
    
    public record CreateSectionCommand(
        @NotBlank String name,
        @PositiveOrZero int displayOrder
    ) {}
    
    public record UpdateSectionCommand(
        @NotBlank String name,
        @PositiveOrZero int displayOrder
    ) {}
    
    public record CreateItemCommand(
        @NotNull UUID sectionId,
        @NotBlank String name,
        String description,
        @NotNull @PositiveOrZero BigDecimal price,
        String imageUrl,
        Set<DietaryTag> dietaryTags,
        @PositiveOrZero int displayOrder
    ) {}
    
    public record UpdateItemCommand(
        @NotNull UUID sectionId,
        @NotBlank String name,
        String description,
        @NotNull @PositiveOrZero BigDecimal price,
        String imageUrl,
        Set<DietaryTag> dietaryTags,
        @PositiveOrZero int displayOrder
    ) {}
    
    public record AvailabilityCommand(
        boolean available
    ) {}
    
    public record UpdateRestaurantCommand(
        @NotBlank String name,
        @NotBlank String slug
    ) {}
    
    public record MenuResponse(
        String tenantId,
        String restaurantName,
        String slug,
        List<SectionResponse> sections
    ) {}
    
    public record SectionResponse(
        String id,
        String name,
        int displayOrder,
        List<ItemResponse> items
    ) {}
    
    public record ItemResponse(
        String id,
        String sectionId,
        String name,
        String description,
        String price,
        String imageUrl,
        boolean available,
        Set<DietaryTag> dietaryTags,
        int displayOrder
    ) {}
}
