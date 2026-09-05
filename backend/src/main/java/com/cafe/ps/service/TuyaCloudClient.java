package com.cafe.ps.service;

import com.cafe.ps.config.TuyaProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Small official Tuya Cloud OpenAPI client. It owns signing, token caching,
 * HTTP timeouts, and response parsing so business services never see Tuya JSON.
 */
@Component
public class TuyaCloudClient {

    private static final String SIGN_METHOD = "HMAC-SHA256";
    private static final int MAX_ALLOWED_ATTEMPTS = 3;

    private final TuyaProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Object tokenMonitor = new Object();
    private volatile CachedToken cachedToken;

    public TuyaCloudClient(TuyaProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(safeTimeout(properties.getConnectTimeout(), 2))
                .build();
    }

    public void sendCommand(String deviceId, String powerCode, boolean value) {
        String body;
        try {
            body = objectMapper.writeValueAsString(Map.of(
                    "commands", List.of(Map.of("code", powerCode, "value", value))
            ));
        } catch (IOException exception) {
            throw new TuyaCloudException(
                    "Could not encode the Tuya device command",
                    null,
                    false,
                    false
            );
        }

        request("POST", "/v1.0/devices/" + pathSegment(deviceId) + "/commands", body);
    }

    public List<TuyaStatusEntry> getStatus(String deviceId) {
        JsonNode result = request(
                "GET",
                "/v1.0/devices/" + pathSegment(deviceId) + "/status",
                ""
        );
        List<TuyaStatusEntry> statuses = new ArrayList<>();
        if (result.isArray()) {
            for (JsonNode item : result) {
                statuses.add(new TuyaStatusEntry(
                        text(item, "code"),
                        jsonValue(item.get("value"))
                ));
            }
        }
        return statuses;
    }

    public TuyaDeviceFunctions getFunctions(String deviceId) {
        JsonNode result = request(
                "GET",
                "/v1.0/devices/" + pathSegment(deviceId) + "/functions",
                ""
        );
        List<String> codes = new ArrayList<>();
        JsonNode functions = result.path("functions");
        if (functions.isArray()) {
            for (JsonNode function : functions) {
                String code = text(function, "code");
                if (code != null && !code.isBlank()) codes.add(code);
            }
        }
        return new TuyaDeviceFunctions(text(result, "category"), List.copyOf(codes));
    }

    private JsonNode request(String method, String path, String body) {
        requireConfiguration();
        int attempts = Math.max(1, Math.min(MAX_ALLOWED_ATTEMPTS, properties.getMaxAttempts()));
        TuyaCloudException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            String token;
            try {
                token = accessToken();
            } catch (TuyaCloudException failure) {
                throw failure;
            }

            try {
                return sendSignedRequest(method, path, body, token);
            } catch (TuyaCloudException failure) {
                lastFailure = failure;
                if (failure.isAuthenticationFailure() && attempt < attempts) {
                    invalidateToken(token);
                    continue;
                }
                if (failure.isConnectivityFailure() && attempt < attempts) {
                    boundedBackoff(attempt);
                    continue;
                }
                throw failure;
            }
        }

        throw lastFailure == null
                ? new TuyaCloudException("Tuya request failed", null, true, false)
                : lastFailure;
    }

    private JsonNode sendSignedRequest(String method, String path, String body, String accessToken) {
        long timestamp = System.currentTimeMillis();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String stringToSign = stringToSign(method, body, path);
        String signatureInput = properties.getClientId()
                + accessToken
                + timestamp
                + nonce
                + stringToSign;
        String signature = hmacSha256(signatureInput, properties.getClientSecret());

        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(safeTimeout(properties.getRequestTimeout(), 5))
                .header("client_id", properties.getClientId())
                .header("access_token", accessToken)
                .header("t", Long.toString(timestamp))
                .header("sign", signature)
                .header("sign_method", SIGN_METHOD)
                .header("nonce", nonce)
                .header("Content-Type", "application/json")
                .method(method, body.isEmpty()
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .build();
        return send(request, false);
    }

    private String accessToken() {
        CachedToken token = cachedToken;
        if (token != null && token.validAt(Instant.now())) return token.value();

        synchronized (tokenMonitor) {
            token = cachedToken;
            if (token != null && token.validAt(Instant.now())) return token.value();

            long timestamp = System.currentTimeMillis();
            String nonce = UUID.randomUUID().toString().replace("-", "");
            String path = "/v1.0/token?grant_type=1";
            String stringToSign = stringToSign("GET", "", path);
            String signatureInput = properties.getClientId()
                    + timestamp
                    + nonce
                    + stringToSign;
            String signature = hmacSha256(signatureInput, properties.getClientSecret());

            HttpRequest request = HttpRequest.newBuilder(uri(path))
                    .timeout(safeTimeout(properties.getRequestTimeout(), 5))
                    .header("client_id", properties.getClientId())
                    .header("t", Long.toString(timestamp))
                    .header("sign", signature)
                    .header("sign_method", SIGN_METHOD)
                    .header("nonce", nonce)
                    .build();
            JsonNode result = send(request, true);
            String value = text(result, "access_token");
            long expiresIn = result.path("expire_time").asLong(0);
            if (value == null || value.isBlank() || expiresIn <= 0) {
                throw new TuyaCloudException(
                        "Tuya authentication returned no usable access token",
                        null,
                        false,
                        true
                );
            }
            // Respect the server-provided lifetime; refresh shortly before it expires.
            long safetyWindow = Math.min(60, Math.max(1, expiresIn / 5));
            cachedToken = new CachedToken(
                    value,
                    Instant.now().plusSeconds(Math.max(1, expiresIn - safetyWindow))
            );
            return value;
        }
    }

    private JsonNode send(HttpRequest request, boolean tokenRequest) {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TuyaCloudException("Tuya request was interrupted", null, true, false);
        } catch (IOException exception) {
            throw new TuyaCloudException("Tuya Cloud is unreachable or timed out", null, true, false);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.body());
        } catch (IOException | RuntimeException exception) {
            throw new TuyaCloudException(
                    "Tuya returned a malformed response",
                    response.statusCode(),
                    response.statusCode() >= 500,
                    false
            );
        }

        if (root == null || root.isNull() || !root.isObject()) {
            throw new TuyaCloudException(
                    "Tuya returned a malformed response",
                    response.statusCode(),
                    response.statusCode() >= 500,
                    false
            );
        }

        boolean success = root.path("success").asBoolean(false);
        int apiCode = root.path("code").asInt(response.statusCode());
        if (response.statusCode() >= 200 && response.statusCode() < 300 && success) {
            return root.path("result");
        }

        String message = safeMessage(root.path("msg").asText(null));
        boolean authenticationFailure = apiCode == 1002
                || apiCode == 1004
                || apiCode == 1010
                || apiCode == 1011;
        boolean connectivityFailure = response.statusCode() == 408
                || response.statusCode() == 429
                || response.statusCode() >= 500;
        if (tokenRequest && authenticationFailure) {
            message = "Tuya authentication failed";
        }
        throw new TuyaCloudException(message, apiCode, connectivityFailure, authenticationFailure);
    }

    private void requireConfiguration() {
        if (!properties.isEnabled()) {
            throw new TuyaCloudException("Tuya Cloud integration is disabled", null, false, false);
        }
        if (properties.getEndpoint() == null
                || properties.getClientId() == null
                || properties.getClientId().isBlank()
                || properties.getClientSecret() == null
                || properties.getClientSecret().isBlank()) {
            throw new TuyaCloudException(
                    "Tuya Cloud is enabled but its endpoint/client credentials are not configured",
                    null,
                    false,
                    false
            );
        }
    }

    private URI uri(String path) {
        String endpoint = properties.getEndpoint().toString();
        while (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        try {
            return URI.create(endpoint + path);
        } catch (IllegalArgumentException exception) {
            throw new TuyaCloudException("Tuya endpoint configuration is invalid", null, false, false);
        }
    }

    private static String pathSegment(String value) {
        if (value == null || value.isBlank() || !value.matches("[A-Za-z0-9_-]+")) {
            throw new TuyaCloudException("Tuya device ID is invalid", null, false, false);
        }
        return value;
    }

    private static String stringToSign(String method, String body, String path) {
        return method.toUpperCase()
                + "\n"
                + sha256(body)
                + "\n\n"
                + path;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return hex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String hmacSha256(String value, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return hex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8))).toUpperCase();
        } catch (Exception exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static Object jsonValue(JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isBoolean()) return node.booleanValue();
        if (node.isNumber()) return node.numberValue();
        return node.asText();
    }

    private static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.get(field) == null || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }

    private static String safeMessage(String message) {
        if (message == null || message.isBlank()) return "Tuya Cloud request failed";
        String sanitized = message.replaceAll("[\\r\\n\\t]+", " ").trim();
        return sanitized.length() <= 240 ? sanitized : sanitized.substring(0, 240);
    }

    private static java.time.Duration safeTimeout(java.time.Duration configured, long fallbackSeconds) {
        if (configured == null || configured.isZero() || configured.isNegative()) {
            return java.time.Duration.ofSeconds(fallbackSeconds);
        }
        return configured;
    }

    private static void boundedBackoff(int attempt) {
        long millis = Math.min(250, 50L * attempt + ThreadLocalRandom.current().nextLong(25));
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TuyaCloudException("Tuya retry was interrupted", null, true, false);
        }
    }

    private void invalidateToken(String token) {
        synchronized (tokenMonitor) {
            if (cachedToken != null && cachedToken.value().equals(token)) cachedToken = null;
        }
    }

    private record CachedToken(String value, Instant refreshAt) {
        private boolean validAt(Instant now) {
            return now.isBefore(refreshAt);
        }
    }
}
