package com.menudigital.application.tenant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * HTTP request body for POST /api/auth/register. Email and Cognito sub are
 * derived from the verified Bearer token, not from this body.
 */
public record RegisterRestaurantRequest(
    @NotBlank @Size(min = 2, max = 100)
    String restaurantName,

    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    String slug
) {}
