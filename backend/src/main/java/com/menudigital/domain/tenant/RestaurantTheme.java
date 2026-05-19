package com.menudigital.domain.tenant;

public record RestaurantTheme(
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
) {
    public static RestaurantTheme defaultTheme() {
        return new RestaurantTheme(
            "#374151",
            "#6b7280", 
            "#9ca3af",
            "#f9fafb",
            "#111827",
            "#ffffff",
            "#374151",
            "#1f2937",
            "to-br",
            "Inter",
            "lg",
            null,
            null,
            true
        );
    }
}
