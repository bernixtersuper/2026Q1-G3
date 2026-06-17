package com.menudigital.infrastructure.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.Unremovable;
import io.quarkus.credentials.CredentialsProvider;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Usuario y contraseña JDBC desde AWS Secrets Manager si {@code menudigital.datasource.secret-arn}
 * está definido; si no, usa los valores de configuración (equivalente a {@code DB_USER}/{@code DB_PASS}).
 */
@ApplicationScoped
@Unremovable
@Named("menudigital-db-credentials")
public class SecretsManagerDbCredentialsProvider implements CredentialsProvider {

    private static final Logger LOG = Logger.getLogger(SecretsManagerDbCredentialsProvider.class);

    private static final String PROVIDER_NAME = "menudigital-db";

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "menudigital.datasource.secret-arn")
    Optional<String> secretArn;

    @ConfigProperty(name = "menudigital.datasource.fallback-username")
    String fallbackUsername;

    @ConfigProperty(name = "menudigital.datasource.fallback-password")
    String fallbackPassword;

    @ConfigProperty(name = "menudigital.datasource.secret-cache-seconds", defaultValue = "300")
    int cacheTtlSeconds;

    @ConfigProperty(name = "aws.dynamodb.region", defaultValue = "us-east-1")
    String awsRegion;

    /** Number of attempts for the Secrets Manager fetch before giving up (resilient cold start). */
    private static final int MAX_FETCH_ATTEMPTS = 4;

    private final AtomicReference<CacheEntry> cache = new AtomicReference<>();

    /**
     * Cliente reutilizado. Construir un {@link SecretsManagerClient} (con su cadena de credenciales
     * y pool HTTP) en cada llamada es costoso y, en arranque en frío bajo concurrencia, la primera
     * llamada se vuelve lo bastante lenta como para que el hilo de agroal la interrumpa, abortando
     * el arranque. Un único cliente perezoso evita ese coste repetido.
     */
    private volatile SecretsManagerClient client;

    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {
        if (!PROVIDER_NAME.equals(credentialsProviderName)) {
            throw new IllegalArgumentException("Unexpected credentials provider: " + credentialsProviderName);
        }
        String arn = secretArn.map(String::trim).filter(s -> !s.isEmpty()).orElse(null);
        if (arn == null) {
            return Map.of(
                USER_PROPERTY_NAME, fallbackUsername,
                PASSWORD_PROPERTY_NAME, fallbackPassword
            );
        }
        long now = System.currentTimeMillis() / 1000L;
        CacheEntry hit = cache.get();
        if (hit != null && hit.expiresAtEpochSeconds() > now) {
            return Map.of(
                USER_PROPERTY_NAME, hit.username(),
                PASSWORD_PROPERTY_NAME, hit.password()
            );
        }
        synchronized (this) {
            hit = cache.get();
            now = System.currentTimeMillis() / 1000L;
            if (hit != null && hit.expiresAtEpochSeconds() > now) {
                return Map.of(USER_PROPERTY_NAME, hit.username(), PASSWORD_PROPERTY_NAME, hit.password());
            }
            UserPass up = fetchFromSecretsManager(arn);
            cache.set(new CacheEntry(up.username(), up.password(), now + Math.max(30, cacheTtlSeconds)));
            LOG.debugf("Refreshed DB credentials from Secrets Manager (cache %ds)", cacheTtlSeconds);
            return Map.of(USER_PROPERTY_NAME, up.username(), PASSWORD_PROPERTY_NAME, up.password());
        }
    }

    private UserPass fetchFromSecretsManager(String arn) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_FETCH_ATTEMPTS; attempt++) {
            // Limpia el flag de interrupción del hilo antes de cada intento: si agroal interrumpió
            // el hilo durante el arranque, el SDK abortaría inmediatamente (abortIfNeeded) sin
            // siquiera intentar la llamada. Reintentar con el flag limpio permite auto-recuperarse.
            Thread.interrupted();
            try {
                String json = client().getSecretValue(
                    GetSecretValueRequest.builder().secretId(arn).build()).secretString();
                JsonNode root = objectMapper.readTree(json);
                String user = text(root, "username", "user");
                String pass = text(root, "password");
                if (user == null || user.isBlank() || pass == null) {
                    throw new IllegalStateException(
                        "El secreto debe incluir al menos 'username' (o 'user') y 'password' en JSON (password puede ser cadena vacía)."
                    );
                }
                return new UserPass(user, pass);
            } catch (Exception e) {
                last = new RuntimeException("Fallo al obtener credenciales de base de datos desde Secrets Manager", e);
                LOG.warnf("Intento %d/%d de leer el secreto JDBC desde AWS Secrets Manager falló: %s",
                    attempt, MAX_FETCH_ATTEMPTS, e.toString());
                if (attempt < MAX_FETCH_ATTEMPTS) {
                    try {
                        Thread.sleep(500L * attempt);
                    } catch (InterruptedException ie) {
                        // Mantener el bucle vivo durante el arranque; se vuelve a limpiar al inicio del intento.
                        Thread.interrupted();
                    }
                }
            }
        }
        LOG.errorf(last, "No se pudo leer el secreto JDBC desde AWS Secrets Manager tras %d intentos: %s",
            MAX_FETCH_ATTEMPTS, arn);
        throw last;
    }

    /** Construye perezosamente (y reutiliza) un único cliente con timeouts y reintentos acotados. */
    private SecretsManagerClient client() {
        SecretsManagerClient c = client;
        if (c == null) {
            synchronized (this) {
                c = client;
                if (c == null) {
                    c = SecretsManagerClient.builder()
                        .region(Region.of(awsRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create())
                        .overrideConfiguration(ClientOverrideConfiguration.builder()
                            .apiCallTimeout(Duration.ofSeconds(10))
                            .apiCallAttemptTimeout(Duration.ofSeconds(4))
                            .retryPolicy(RetryPolicy.builder().numRetries(3).build())
                            .build())
                        .build();
                    client = c;
                }
            }
        }
        return c;
    }

    @PreDestroy
    void closeClient() {
        SecretsManagerClient c = client;
        if (c != null) {
            c.close();
        }
    }

    private static String text(JsonNode root, String... fieldNames) {
        for (String field : fieldNames) {
            JsonNode n = root.get(field);
            if (n != null && n.isTextual()) {
                String v = n.asText();
                if (v != null && !v.isBlank()) {
                    return v;
                }
            }
        }
        return null;
    }

    private record CacheEntry(String username, String password, long expiresAtEpochSeconds) {}

    private record UserPass(String username, String password) {}
}
