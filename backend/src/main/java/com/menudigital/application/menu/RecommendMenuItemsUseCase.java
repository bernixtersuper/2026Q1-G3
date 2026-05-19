package com.menudigital.application.menu;

import com.menudigital.domain.menu.Menu;
import com.menudigital.infrastructure.ml.RecommendationModelLoader;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sugerencias de hasta 3 ítems fuera del carrito. Si existe el artefacto MREC (popularidad) en S3 para el tenant
 * ({@link RecommendationModelLoader}), se priorizan ítems con más vistas; si no, orden aleatorio.
 */
@ApplicationScoped
public class RecommendMenuItemsUseCase {

    private static final Logger LOG = Logger.getLogger(RecommendMenuItemsUseCase.class);

    @Inject
    GetPublicMenuUseCase getPublicMenuUseCase;

    @Inject
    RecommendationModelLoader recommendationModelLoader;

    public List<String> execute(String slug, List<String> itemsInCart, List<String> menuItemIdsFromClient) {
        var menuOpt = getPublicMenuUseCase.execute(slug);
        if (menuOpt.isEmpty()) {
            return List.of();
        }
        var menu = menuOpt.get();
        String tenantId = menu.getTenantId() != null ? menu.getTenantId().toString() : null;

        Optional<Map<String, Integer>> popularity = recommendationModelLoader.itemPopularityForTenant(tenantId);
        if (popularity.isPresent() && !popularity.get().isEmpty()) {
            LOG.tracef("Recommendations using popularity map (%d entries) for tenant %s",
                popularity.get().size(), tenantId);
        }

        List<String> candidates = resolveCandidates(menuItemIdsFromClient, menu);
        Set<String> cart = new HashSet<>();
        if (itemsInCart != null) {
            itemsInCart.stream().filter(id -> id != null && !id.isBlank()).forEach(cart::add);
        }
        List<String> pool = candidates.stream()
            .filter(id -> id != null && !id.isBlank() && !cart.contains(id))
            .toList();
        if (pool.isEmpty()) {
            return List.of();
        }
        int k = Math.min(3, pool.size());
        List<String> ordered = new ArrayList<>(pool);
        Map<String, Integer> scores = popularity.orElse(Map.of());
        if (!scores.isEmpty()) {
            Collections.shuffle(ordered, ThreadLocalRandom.current());
            ordered.sort(Comparator.comparing((String id) -> scores.getOrDefault(id, 0)).reversed());
        } else {
            Collections.shuffle(ordered, ThreadLocalRandom.current());
        }
        return ordered.subList(0, k);
    }

    private List<String> resolveCandidates(List<String> menuItemIdsFromClient, Menu menu) {
        if (menuItemIdsFromClient != null && !menuItemIdsFromClient.isEmpty()) {
            return List.copyOf(menuItemIdsFromClient);
        }
        return menu.getSortedSections().stream()
            .flatMap(s -> s.getItems().stream())
            .map(item -> item.getId().toString())
            .toList();
    }
}
