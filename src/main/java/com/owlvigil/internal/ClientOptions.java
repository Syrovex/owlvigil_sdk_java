package com.owlvigil.internal;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import okhttp3.OkHttpClient;

public final class ClientOptions {
    public final String baseUrl;
    public final Supplier<String> credentialProvider;
    public final Duration timeout;
    public final int maxRetries;
    public final String userAgent;
    public final ObjectMapper mapper;
    public final OkHttpClient httpClient;
    public final boolean ownsHttpClient;
    public final boolean unwrapEnvelope;

    public ClientOptions(
            String baseUrl,
            Supplier<String> credentialProvider,
            Duration timeout,
            int maxRetries,
            String userAgent,
            ObjectMapper mapper,
            OkHttpClient httpClient,
            boolean ownsHttpClient,
            boolean unwrapEnvelope) {
        this.baseUrl = trimTrailingSlash(Objects.requireNonNull(baseUrl, "baseUrl"));
        this.credentialProvider = Objects.requireNonNull(credentialProvider, "credentialProvider");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        this.maxRetries = maxRetries;
        this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.ownsHttpClient = ownsHttpClient;
        this.unwrapEnvelope = unwrapEnvelope;
    }

    private static String trimTrailingSlash(String value) {
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
        return value;
    }
}
