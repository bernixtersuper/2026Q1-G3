package com.menudigital.infrastructure.ml;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Formato binario {@code MREC} (popularidad por ítem + metadatos) generado por el ETL Python.
 * No es pickle/joblib: Java puede leerlo sin runtime Python.
 */
public final class RecommendationArtifactBinaryCodec {

    private static final int MAGIC = 0x4d524543; // 'M','R','E','C' big-endian int

    private RecommendationArtifactBinaryCodec() {}

    /**
     * Decodifica el cuerpo del objeto S3. Devuelve mapa vacío si magic o versión no coinciden.
     */
    public static Map<String, Integer> decodeItemPopularity(byte[] data) {
        if (data == null || data.length < 8) {
            return Map.of();
        }
        ByteBuffer buf = ByteBuffer.wrap(data);
        if (buf.getInt() != MAGIC) {
            return Map.of();
        }
        int version = buf.getInt();
        if (version != 4) {
            return Map.of();
        }
        readUtf(buf); // trained_at (ignored for ranking)
        readUtf(buf); // source_day
        readUtf(buf); // tenant_id
        int n = buf.getInt();
        if (n < 0 || n > 1_000_000) {
            return Map.of();
        }
        Map<String, Integer> out = new HashMap<>();
        for (int i = 0; i < n; i++) {
            String itemId = readUtf(buf);
            int count = buf.getInt();
            if (!itemId.isEmpty()) {
                out.put(itemId, count);
            }
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static String readUtf(ByteBuffer buf) {
        if (buf.remaining() < 4) {
            return "";
        }
        int len = buf.getInt();
        if (len < 0 || len > buf.remaining()) {
            return "";
        }
        byte[] raw = new byte[len];
        buf.get(raw);
        return new String(raw, StandardCharsets.UTF_8);
    }
}
