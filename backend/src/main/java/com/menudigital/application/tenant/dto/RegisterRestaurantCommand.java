package com.menudigital.application.tenant.dto;

/**
 * Use-case input. Built by the AuthResource from a validated request body plus
 * claims extracted from a verified Cognito ID token. Not bound directly to the
 * HTTP layer — see {@link RegisterRestaurantRequest} for the request body shape.
 */
public record RegisterRestaurantCommand(
    String restaurantName,
    String slug,
    String ownerEmail,
    String cognitoSub
) {}
