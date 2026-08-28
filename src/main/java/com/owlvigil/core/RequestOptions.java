package com.owlvigil.core;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

public final class RequestOptions {
    private static final Set<String> RESERVED_HEADERS = Set.of(
            "authorization", "host", "content-length", "content-type", "idempotency-key");
    private static final RequestOptions DEFAULT = builder().build();

    private final Long workspaceId;
    private final String idempotencyKey;
    private final Duration timeout;
    private final Integer maxRetries;
    private final Map<String, String> headers;

    private RequestOptions(Builder builder) {
        this.workspaceId = builder.workspaceId;
        this.idempotencyKey = builder.idempotencyKey;
        this.timeout = builder.timeout;
        this.maxRetries = builder.maxRetries;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
    }

    public static RequestOptions defaults() { return DEFAULT; }
    public static Builder builder() { return new Builder(); }
    public Long workspaceId() { return workspaceId; }
    public String idempotencyKey() { return idempotencyKey; }
    public Duration timeout() { return timeout; }
    public Integer maxRetries() { return maxRetries; }
    public Map<String, String> headers() { return headers; }

    public static final class Builder {
        private Long workspaceId;
        private String idempotencyKey;
        private Duration timeout;
        private Integer maxRetries;
        private final Map<String, String> headers = new LinkedHashMap<>();

        public Builder workspaceId(long workspaceId) {
            if (workspaceId <= 0) throw new IllegalArgumentException("workspaceId must be positive");
            this.workspaceId = workspaceId;
            return this;
        }

        public Builder idempotencyKey(String value) {
            this.idempotencyKey = requireNonBlank(value, "idempotencyKey");
            return this;
        }

        public Builder timeout(Duration value) {
            if (value == null || value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException("timeout must be positive");
            }
            this.timeout = value;
            return this;
        }

        public Builder maxRetries(int value) {
            if (value < 0) throw new IllegalArgumentException("maxRetries must not be negative");
            this.maxRetries = value;
            return this;
        }

        public Builder header(String name, String value) {
            String checkedName = requireNonBlank(name, "header name");
            if (RESERVED_HEADERS.contains(checkedName.toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("Header " + checkedName + " is managed by the SDK");
            }
            headers.put(checkedName, requireNonBlank(value, "header value"));
            return this;
        }

        public RequestOptions build() { return new RequestOptions(this); }

        private static String requireNonBlank(String value, String name) {
            if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
            return value;
        }
    }
}
