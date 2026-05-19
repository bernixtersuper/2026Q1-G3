package com.menudigital.infrastructure.storage;

import java.util.regex.Pattern;

/**
 * Claves S3 en BD ({@code menus/{tenantId}/{uuid}.ext}); URLs públicas vía proxy {@code /api/media/...}.
 */
@jakarta.enterprise.context.ApplicationScoped
public class MenuImageUrls {

    private static final String KEY_PREFIX = "menus/";
    private static final String API_PREFIX = "/api/media/";
    private static final Pattern VALID_KEY = Pattern.compile(
        "^menus/[^/]+/[^/]+\\.(jpg|jpeg|png|gif|webp)$",
        Pattern.CASE_INSENSITIVE
    );

    public boolean isValidObjectKey(String objectKey) {
        return objectKey != null && VALID_KEY.matcher(objectKey).matches();
    }

    /** Normaliza lo que envía el cliente o hay en BD a clave S3 (o vacío). */
    public String normalizeForStorage(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith(API_PREFIX)) {
            trimmed = trimmed.substring(API_PREFIX.length());
        }
        int menusIdx = trimmed.indexOf(KEY_PREFIX);
        if (menusIdx >= 0) {
            trimmed = trimmed.substring(menusIdx);
        }
        int q = trimmed.indexOf('?');
        if (q >= 0) {
            trimmed = trimmed.substring(0, q);
        }
        return trimmed.startsWith(KEY_PREFIX) ? trimmed : "";
    }

    /** Ruta relativa al API para &lt;img src&gt; (el frontend antepone {@code VITE_API_URL}). */
    public String toApiPath(String stored) {
        String key = normalizeForStorage(stored);
        if (key.isEmpty()) {
            return "";
        }
        return API_PREFIX + key;
    }
}
