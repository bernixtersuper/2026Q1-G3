package com.menudigital.infrastructure.ml;

import com.menudigital.infrastructure.storage.S3ClientFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Descarga bajo demanda el artefacto de recomendaciones (binario {@code MREC}) por tenant desde S3.
 * El ETL también publica un .joblib con el mismo contenido + metadatos ML para Python;
 * la API solo consume el binario {@code MREC}.
 */
@ApplicationScoped
public class RecommendationModelLoader {

    private static final Logger LOG = Logger.getLogger(RecommendationModelLoader.class);
    private static final String TENANT_PLACEHOLDER = "{tenantId}";
    /** Clave S3 del .bin; debe coincidir con el worker ML y el ETL. */
    private static final String MODEL_S3_KEY_PATTERN = "recommendations/{tenantId}/model.bin";

    @Inject
    S3ClientFactory s3ClientFactory;

    @ConfigProperty(name = "recommendations.model.s3.bucket")
    Optional<String> modelBucket;

    private final ConcurrentHashMap<String, Map<String, Integer>> cache = new ConcurrentHashMap<>();

    private boolean configured() {
        return modelBucket.map(String::trim).filter(s -> !s.isEmpty()).isPresent();
    }

    /**
     * Popularidad por {@code itemId} (vistas ITEM_VIEW agregadas en el ETL). Vacío si no hay objeto o formato inválido.
     */
    public Optional<Map<String, Integer>> itemPopularityForTenant(String tenantId) {
        if (tenantId == null || tenantId.isBlank() || !configured()) {
            return Optional.empty();
        }
        Map<String, Integer> cached = cache.get(tenantId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<Map<String, Integer>> loaded = loadPopularityForTenant(tenantId);
        loaded.ifPresent(m -> cache.put(tenantId, m));
        return loaded;
    }

    private Optional<Map<String, Integer>> loadPopularityForTenant(String tenantId) {
        String bucket = modelBucket.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
        if (bucket == null) {
            return Optional.empty();
        }
        String key = MODEL_S3_KEY_PATTERN.replace(TENANT_PLACEHOLDER, tenantId);
        try (S3Client s3 = s3ClientFactory.createClient()) {
            var response = s3.getObjectAsBytes(
                GetObjectRequest.builder().bucket(bucket).key(key).build()
            );
            byte[] bytes = response.asByteArray();
            if (bytes.length == 0) {
                LOG.warnf("Recommendation model object is empty: s3://%s/%s", bucket, key);
                return Optional.empty();
            }
            Map<String, Integer> map = RecommendationArtifactBinaryCodec.decodeItemPopularity(bytes);
            if (map.isEmpty()) {
                LOG.debugf("Recommendation model has no item_popularity entries: s3://%s/%s", bucket, key);
            } else {
                LOG.infof("Loaded recommendation popularity for tenant %s from s3://%s/%s (%d items)",
                    tenantId, bucket, key, map.size());
            }
            return Optional.of(map);
        } catch (NoSuchKeyException e) {
            LOG.debugf("No recommendation model for tenant %s (s3://%s/%s)", tenantId, bucket, key);
            return Optional.empty();
        } catch (Exception e) {
            LOG.errorf(e, "Failed to load recommendation model for tenant %s from s3://%s/%s", tenantId, bucket, key);
            return Optional.empty();
        }
    }
}
