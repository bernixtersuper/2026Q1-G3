package com.menudigital.infrastructure.auth;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.smallrye.jwt.auth.principal.JWTParser;
import io.smallrye.jwt.auth.principal.ParseException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;

@ApplicationScoped
public class CognitoTokenVerifier {

    private static final String BEARER_PREFIX = "Bearer ";

    @Inject
    JWTParser parser;

    @ConfigProperty(name = "cognito.issuer-url", defaultValue = "")
    String issuerUrl;

    @ConfigProperty(name = "cognito.client-id", defaultValue = "")
    String clientId;

    private volatile JWTAuthContextInfo contextInfo;

    public boolean isConfigured() {
        return !issuerUrl.isBlank() && !clientId.isBlank();
    }

    public JsonWebToken verify(String token) throws ParseException {
        if (!isConfigured()) {
            throw new ParseException("Cognito is not configured (set COGNITO_ISSUER_URL and COGNITO_CLIENT_ID)");
        }
        return parser.parse(token, contextInfo());
    }

    /**
     * Verifies a Cognito ID token from an Authorization header. Checks signature,
     * issuer, expiry, audience (== client_id), token_use (== "id"), and that the
     * email is present and verified by the upstream IdP.
     */
    public VerifiedIdToken verifyIdTokenHeader(String authorizationHeader) throws AuthenticationException {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AuthenticationException("Missing Bearer token");
        }
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            throw new AuthenticationException("Empty Bearer token");
        }

        JsonWebToken jwt;
        try {
            jwt = verify(token);
        } catch (ParseException e) {
            throw new AuthenticationException("Invalid token: " + e.getMessage());
        }

        String tokenUse = jwt.getClaim("token_use");
        if (!"id".equals(tokenUse)) {
            throw new AuthenticationException("Expected ID token (token_use=id), got: " + tokenUse);
        }

        Set<String> audiences = jwt.getAudience();
        if (audiences == null || !audiences.contains(clientId)) {
            throw new AuthenticationException("Token audience does not match configured client_id");
        }

        Boolean emailVerified = jwt.getClaim("email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new AuthenticationException("Email is not verified by the upstream identity provider");
        }

        String email = jwt.getClaim("email");
        String sub = jwt.getSubject();
        if (email == null || email.isBlank() || sub == null || sub.isBlank()) {
            throw new AuthenticationException("Token is missing email or sub claim");
        }

        return new VerifiedIdToken(email, sub);
    }

    public String expectedClientId() {
        return clientId;
    }

    private JWTAuthContextInfo contextInfo() {
        JWTAuthContextInfo cached = contextInfo;
        if (cached == null) {
            cached = new JWTAuthContextInfo();
            cached.setIssuedBy(issuerUrl);
            cached.setPublicKeyLocation(issuerUrl + "/.well-known/jwks.json");
            contextInfo = cached;
        }
        return cached;
    }

    public record VerifiedIdToken(String email, String sub) {}

    public static class AuthenticationException extends Exception {
        public AuthenticationException(String message) {
            super(message);
        }
    }
}
