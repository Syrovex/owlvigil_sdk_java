package com.owlvigil;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlvigil.gateway.GatewayClient;
import com.owlvigil.internal.ClientOptions;
import com.owlvigil.internal.HttpTransport;
import com.owlvigil.management.ManagementClient;
import com.owlvigil.oauth2.OAuth2Client;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import okhttp3.OkHttpClient;

public final class OwlVigilClient implements AutoCloseable {
    public static final String VERSION = "0.3.0";
    public static final String DEFAULT_GATEWAY_BASE_URL = "https://gateway.owlvigil.com";
    public static final String DEFAULT_MANAGEMENT_BASE_URL = "https://api.owlvigil.com/v1";
    public static final String DEFAULT_OAUTH_BASE_URL = "https://open.owlvigil.com";

    private final OkHttpClient httpClient;
    private final boolean ownsHttpClient;
    private final GatewayClient gateway;
    private final ManagementClient management;
    private final OAuth2Client oauth2;

    private OwlVigilClient(Builder builder) {
        ObjectMapper mapper = builder.mapper == null
                ? new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                : builder.mapper;
        this.ownsHttpClient = builder.httpClient == null;
        this.httpClient = ownsHttpClient
                ? new OkHttpClient.Builder()
                        .connectTimeout(builder.timeout)
                        .readTimeout(builder.timeout)
                        .writeTimeout(builder.timeout)
                        .build()
                : builder.httpClient;

        HttpTransport gatewayTransport = new HttpTransport(new ClientOptions(
                builder.gatewayBaseUrl,
                requireProvider(builder.gatewayKeyProvider, "gateway key"),
                builder.timeout,
                builder.maxRetries,
                builder.userAgent,
                mapper,
                httpClient,
                false,
                false));
        HttpTransport managementTransport = new HttpTransport(new ClientOptions(
                builder.managementBaseUrl,
                requireProvider(builder.apiKeyProvider, "management API key"),
                builder.timeout,
                builder.maxRetries,
                builder.userAgent,
                mapper,
                httpClient,
                false,
                true));
        this.gateway = new GatewayClient(gatewayTransport, mapper);
        this.management = new ManagementClient(managementTransport);
        this.oauth2 = new OAuth2Client(builder.oauthBaseUrl, httpClient, mapper, builder.timeout);
    }

    public static Builder builder() { return new Builder(); }
    public GatewayClient gateway() { return gateway; }
    public ManagementClient management() { return management; }
    public OAuth2Client oauth2() { return oauth2; }

    @Override
    public void close() {
        if (!ownsHttpClient) return;
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
        if (httpClient.cache() != null) {
            try {
                httpClient.cache().close();
            } catch (Exception ignored) {
                // Closing remains best effort.
            }
        }
    }

    private static Supplier<String> requireProvider(Supplier<String> provider, String label) {
        return () -> {
            String value = provider == null ? null : provider.get();
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException("OwlVigil " + label + " is required for this client");
            }
            return value;
        };
    }

    public static final class Builder {
        private Supplier<String> gatewayKeyProvider = () -> System.getenv("OWLVIGIL_GATEWAY_KEY");
        private Supplier<String> apiKeyProvider = () -> System.getenv("OWLVIGIL_API_KEY");
        private String gatewayBaseUrl = DEFAULT_GATEWAY_BASE_URL;
        private String managementBaseUrl = DEFAULT_MANAGEMENT_BASE_URL;
        private String oauthBaseUrl = DEFAULT_OAUTH_BASE_URL;
        private Duration timeout = Duration.ofSeconds(60);
        private int maxRetries = 2;
        private String userAgent = "owlvigil-java/" + VERSION;
        private ObjectMapper mapper;
        private OkHttpClient httpClient;

        public Builder gatewayKey(String value) { this.gatewayKeyProvider = () -> value; return this; }
        public Builder gatewayKeyProvider(Supplier<String> value) { this.gatewayKeyProvider = Objects.requireNonNull(value); return this; }
        public Builder apiKey(String value) { this.apiKeyProvider = () -> value; return this; }
        public Builder apiKeyProvider(Supplier<String> value) { this.apiKeyProvider = Objects.requireNonNull(value); return this; }
        public Builder gatewayBaseUrl(String value) { this.gatewayBaseUrl = requireNonBlank(value, "gatewayBaseUrl"); return this; }
        public Builder managementBaseUrl(String value) { this.managementBaseUrl = requireNonBlank(value, "managementBaseUrl"); return this; }
        public Builder oauthBaseUrl(String value) { this.oauthBaseUrl = requireNonBlank(value, "oauthBaseUrl"); return this; }
        public Builder timeout(Duration value) {
            if (value == null || value.isZero() || value.isNegative()) throw new IllegalArgumentException("timeout must be positive");
            this.timeout = value;
            return this;
        }
        public Builder maxRetries(int value) {
            if (value < 0) throw new IllegalArgumentException("maxRetries must not be negative");
            this.maxRetries = value;
            return this;
        }
        public Builder userAgent(String value) { this.userAgent = requireNonBlank(value, "userAgent"); return this; }
        public Builder objectMapper(ObjectMapper value) { this.mapper = Objects.requireNonNull(value); return this; }
        public Builder httpClient(OkHttpClient value) { this.httpClient = Objects.requireNonNull(value); return this; }
        public OwlVigilClient build() { return new OwlVigilClient(this); }

        private static String requireNonBlank(String value, String name) {
            if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
            return value;
        }
    }
}
