package com.cafe.ps.service;

import com.cafe.ps.config.TuyaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.assertThat;

class TuyaCloudClientTest {

    private static final String CLIENT_ID = "client-id";
    private static final String CLIENT_SECRET = "client-secret";
    private HttpServer server;
    private AtomicInteger tokenRequests;
    private List<RequestCapture> requests;

    @BeforeEach
    void startServer() throws IOException {
        tokenRequests = new AtomicInteger();
        requests = new CopyOnWriteArrayList<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void signsOfficialRequestsAndReusesServerProvidedTokenLifetime() {
        TuyaProperties properties = new TuyaProperties();
        properties.setEnabled(true);
        properties.setEndpoint(URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
        properties.setClientId(CLIENT_ID);
        properties.setClientSecret(CLIENT_SECRET);
        properties.setMaxAttempts(1);

        TuyaCloudClient client = new TuyaCloudClient(properties, new ObjectMapper());

        assertThat(client.getFunctions("device123").codes()).containsExactly("relay_power", "timer");
        assertThat(client.getStatus("device123")).containsExactly(
                new TuyaStatusEntry("relay_power", true)
        );
        client.sendCommand("device123", "relay_power", true);

        assertThat(tokenRequests).hasValue(1);
        List<RequestCapture> business = requests.stream()
                .filter(request -> !request.path().startsWith("/v1.0/token"))
                .toList();
        assertThat(business).hasSize(3);
        assertThat(business).allSatisfy(request -> {
            assertThat(request.headers().getFirst("client_id")).isEqualTo(CLIENT_ID);
            assertThat(request.headers().getFirst("access_token")).isEqualTo("test-access-token");
            assertThat(request.headers().getFirst("sign")).isNotBlank();
            assertThat(request.body()).doesNotContain(CLIENT_SECRET);
        });

        RequestCapture token = requests.stream()
                .filter(request -> request.path().startsWith("/v1.0/token"))
                .findFirst()
                .orElseThrow();
        assertThat(token.headers().getFirst("sign")).isEqualTo(expectedSignature(
                CLIENT_ID
                        + token.headers().getFirst("t")
                        + token.headers().getFirst("nonce")
                        + "GET\n"
                        + sha256("")
                        + "\n\n/v1.0/token?grant_type=1"
        ));

        RequestCapture command = business.stream()
                .filter(request -> request.path().endsWith("/commands"))
                .findFirst()
                .orElseThrow();
        assertThat(command.body()).contains("relay_power").contains("true");
        assertThat(command.headers().getFirst("sign")).isEqualTo(expectedSignature(
                CLIENT_ID
                        + "test-access-token"
                        + command.headers().getFirst("t")
                        + command.headers().getFirst("nonce")
                        + "POST\n"
                        + sha256(command.body())
                        + "\n\n/v1.0/devices/device123/commands"
        ));
    }

    private void handle(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        requests.add(new RequestCapture(
                exchange.getRequestURI().toString(),
                exchange.getRequestHeaders(),
                body
        ));

        String path = exchange.getRequestURI().getPath();
        String response;
        if (path.equals("/v1.0/token")) {
            tokenRequests.incrementAndGet();
            response = "{\"success\":true,\"result\":{\"access_token\":\"test-access-token\",\"expire_time\":3600}}";
        } else if (path.endsWith("/functions")) {
            response = "{\"success\":true,\"result\":{\"category\":\"cz\",\"functions\":[{\"code\":\"relay_power\"},{\"code\":\"timer\"}]}}";
        } else if (path.endsWith("/status")) {
            response = "{\"success\":true,\"result\":[{\"code\":\"relay_power\",\"value\":true}]}";
        } else if (path.endsWith("/commands")) {
            response = "{\"success\":true,\"result\":true}";
        } else {
            response = "{\"success\":false,\"code\":1000,\"msg\":\"not found\"}";
        }
        byte[] payload = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, payload.length);
        try (var output = exchange.getResponseBody()) {
            output.write(payload);
        }
    }

    private static String expectedSignature(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(CLIENT_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString().toUpperCase();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private record RequestCapture(
            String path,
            com.sun.net.httpserver.Headers headers,
            String body
    ) {
    }
}
