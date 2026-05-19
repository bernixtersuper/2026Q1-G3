package com.menudigital.interfaces.rest;

import com.menudigital.application.shared.TenantContext;
import com.menudigital.domain.tenant.TenantId;
import com.menudigital.infrastructure.persistence.UserRepositoryImpl;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Provider
@Priority(Priorities.AUTHENTICATION + 1)
public class TenantRequestFilter implements ContainerRequestFilter {

    @Inject
    JsonWebToken jwt;

    @Inject
    TenantContext tenantContext;

    @Inject
    UserRepositoryImpl userRepository;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (jwt == null || jwt.getRawToken() == null) {
            return;
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return;
        }

        userRepository.findByCognitoSub(sub).ifPresent(user -> {
            if (user.restaurantId != null) {
                tenantContext.setTenantId(TenantId.of(user.restaurantId.toString()));
            }
        });
    }
}
