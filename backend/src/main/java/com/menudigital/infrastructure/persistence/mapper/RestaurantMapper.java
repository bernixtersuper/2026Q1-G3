package com.menudigital.infrastructure.persistence.mapper;

import com.menudigital.domain.tenant.Restaurant;
import com.menudigital.domain.tenant.RestaurantTheme;
import com.menudigital.domain.tenant.TenantId;
import com.menudigital.infrastructure.persistence.entity.RestaurantEntity;

import java.util.HashMap;
import java.util.Map;

public class RestaurantMapper {
    
    private RestaurantMapper() {
    }
    
    public static Restaurant toDomain(RestaurantEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Restaurant(
            new TenantId(entity.id),
            entity.name,
            entity.slug,
            entity.ownerEmail,
            mapToTheme(entity.theme),
            entity.createdAt
        );
    }
    
    public static RestaurantEntity toEntity(Restaurant domain) {
        if (domain == null) {
            return null;
        }
        RestaurantEntity entity = new RestaurantEntity();
        entity.id = domain.getId().value();
        entity.name = domain.getName();
        entity.slug = domain.getSlug();
        entity.ownerEmail = domain.getOwnerEmail();
        entity.theme = mapFromTheme(domain.getTheme());
        entity.createdAt = domain.getCreatedAt();
        return entity;
    }
    
    public static void updateEntity(RestaurantEntity entity, Restaurant domain) {
        entity.name = domain.getName();
        entity.slug = domain.getSlug();
        entity.ownerEmail = domain.getOwnerEmail();
        entity.theme = mapFromTheme(domain.getTheme());
    }
    
    private static RestaurantTheme mapToTheme(Map<String, Object> themeMap) {
        if (themeMap == null || themeMap.isEmpty()) {
            return RestaurantTheme.defaultTheme();
        }
        RestaurantTheme defaults = RestaurantTheme.defaultTheme();
        return new RestaurantTheme(
            getString(themeMap, "primaryColor", defaults.primaryColor()),
            getString(themeMap, "secondaryColor", defaults.secondaryColor()),
            getString(themeMap, "accentColor", defaults.accentColor()),
            getString(themeMap, "backgroundColor", defaults.backgroundColor()),
            getString(themeMap, "textColor", defaults.textColor()),
            getString(themeMap, "cardBackground", defaults.cardBackground()),
            getString(themeMap, "gradientStart", defaults.gradientStart()),
            getString(themeMap, "gradientEnd", defaults.gradientEnd()),
            getString(themeMap, "gradientDirection", defaults.gradientDirection()),
            getString(themeMap, "fontFamily", defaults.fontFamily()),
            getString(themeMap, "borderRadius", defaults.borderRadius()),
            getString(themeMap, "logoUrl", defaults.logoUrl()),
            getString(themeMap, "bannerUrl", defaults.bannerUrl()),
            getBoolean(themeMap, "showGradientHeader", defaults.showGradientHeader())
        );
    }
    
    private static Map<String, Object> mapFromTheme(RestaurantTheme theme) {
        if (theme == null) {
            theme = RestaurantTheme.defaultTheme();
        }
        Map<String, Object> map = new HashMap<>();
        map.put("primaryColor", theme.primaryColor());
        map.put("secondaryColor", theme.secondaryColor());
        map.put("accentColor", theme.accentColor());
        map.put("backgroundColor", theme.backgroundColor());
        map.put("textColor", theme.textColor());
        map.put("cardBackground", theme.cardBackground());
        map.put("gradientStart", theme.gradientStart());
        map.put("gradientEnd", theme.gradientEnd());
        map.put("gradientDirection", theme.gradientDirection());
        map.put("fontFamily", theme.fontFamily());
        map.put("borderRadius", theme.borderRadius());
        map.put("logoUrl", theme.logoUrl());
        map.put("bannerUrl", theme.bannerUrl());
        map.put("showGradientHeader", theme.showGradientHeader());
        return map;
    }
    
    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private static boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
