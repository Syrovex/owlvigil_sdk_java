package com.owlvigil.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlvigil.core.ApiResponse;
import com.owlvigil.core.RequestOptions;
import com.owlvigil.core.ResponseMeta;
import com.owlvigil.errors.OwlVigilApiException;
import com.owlvigil.errors.OwlVigilException;
import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class HttpTransport implements AutoCloseable {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final ClientOptions options;

    public HttpTransport(ClientOptions options) {
        this.options = options;
    }

    public <T> ApiResponse<T> execute(
            String method,
            String path,
            Object body,
            Class<T> responseType,
            RequestOptions requestOptions) {
        RequestOptions effective = requestOptions == null ? RequestOptions.defaults() : requestOptions;
        int maxRetries = effective.maxRetries() == null ? options.maxRetries : effective.maxRetries();
        int attempt = 0;
        while (true) {
            Request request = buildRequest(method, path, body, effective);
            try (Response response = newCall(request, effective.timeout()).execute()) {
                if (isRetryable(request, response.code(), effective) && attempt < maxRetries) {
                    closeQuietly(response.body());
                    backoff(attempt++, response.header("Retry-After"));
                    continue;
                }
                return decode(response, responseType);
            } catch (IOException error) {
                if (!isRetryable(request, -1, effective) || attempt >= maxRetries) {
                    throw new OwlVigilException("owlvigil: request failed", error);
                }
                backoff(attempt++, null);
            }
        }
    }

    public Response openStream(String path, Object body, RequestOptions requestOptions) {
        RequestOptions effective = requestOptions == null ? RequestOptions.defaults() : requestOptions;
        Request request = buildRequest("POST", path, body, effective).newBuilder()
                .header("Accept", "text/event-stream")
                .build();
        try {
            OkHttpClient streamClient = options.httpClient.newBuilder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .callTimeout(effective.timeout() == null ? options.timeout : effective.timeout())
                    .build();
            Response response = streamClient.newCall(request).execute();
            if (!response.isSuccessful()) {
                try (Response ignored = response) {
                    throw apiException(response, readBody(response.body()));
                }
            }
            return response;
        } catch (IOException error) {
            throw new OwlVigilException("owlvigil: stream request failed", error);
        }
    }

    private Request buildRequest(String method, String path, Object body, RequestOptions requestOptions) {
        String url = options.baseUrl + (path.startsWith("/") ? path : "/" + path);
        if (requestOptions.workspaceId() != null) {
            url += (url.contains("?") ? "&" : "?") + "workspace_id=" + requestOptions.workspaceId();
        }
        Request.Builder builder = new Request.Builder().url(url)
                .header("Authorization", "Bearer " + requireCredential())
                .header("User-Agent", options.userAgent)
                .header("Accept", "application/json");
        requestOptions.headers().forEach(builder::header);
        if (requestOptions.idempotencyKey() != null) {
            builder.header("Idempotency-Key", requestOptions.idempotencyKey());
        }
        RequestBody requestBody = null;
        if (body != null) {
            try {
                requestBody = RequestBody.create(options.mapper.writeValueAsBytes(body), JSON);
            } catch (IOException error) {
                throw new OwlVigilException("owlvigil: encode request", error);
            }
        } else if (requiresBody(method)) {
            requestBody = RequestBody.create(new byte[0], JSON);
        }
        return builder.method(method.toUpperCase(Locale.ROOT), requestBody).build();
    }

    private Call newCall(Request request, Duration requestTimeout) {
        OkHttpClient client = options.httpClient;
        Duration timeout = requestTimeout == null ? options.timeout : requestTimeout;
        if (timeout != null) {
            client = client.newBuilder().callTimeout(timeout).build();
        }
        return client.newCall(request);
    }

    private <T> ApiResponse<T> decode(Response response, Class<T> responseType) throws IOException {
        String payload = readBody(response.body());
        if (!response.isSuccessful()) throw apiException(response, payload);
        T value;
        String responseRequestId = requestId(response);
        if (responseType == Void.class) {
            value = responseType.cast(null);
        } else if (responseType == String.class) {
            value = responseType.cast(payload);
        } else {
            try {
                JsonNode root = options.mapper.readTree(payload.isEmpty() ? "{}" : payload);
                if (options.unwrapEnvelope && root.isObject() && root.has("data")) {
                    if (responseRequestId.isEmpty()) responseRequestId = text(root, "request_id");
                    root = root.get("data");
                }
                value = options.mapper.treeToValue(root, responseType);
            } catch (IOException error) {
                String suffix = responseRequestId.isEmpty() ? "" : " request_id=" + responseRequestId;
                throw new OwlVigilException("owlvigil: invalid JSON response status=" + response.code() + suffix, error);
            }
        }
        ResponseMeta meta = new ResponseMeta(response.code(), responseRequestId, response.headers().toMultimap());
        return new ApiResponse<>(value, meta);
    }

    private OwlVigilApiException apiException(Response response, String payload) {
        String code = "";
        String message = response.message();
        String bodyRequestId = "";
        try {
            JsonNode root = options.mapper.readTree(payload);
            JsonNode error = root.has("error") && root.get("error").isObject() ? root.get("error") : root;
            code = text(error, "code");
            bodyRequestId = text(root, "request_id");
            String parsed = text(error, "message");
            if (!parsed.isEmpty()) message = parsed;
        } catch (Exception ignored) {
            // The exception intentionally omits an unparsed response body.
        }
        String responseRequestId = requestId(response);
        return new OwlVigilApiException(response.code(), responseRequestId.isEmpty() ? bodyRequestId : responseRequestId, code, message);
    }

    private boolean isRetryable(Request request, int status, RequestOptions requestOptions) {
        boolean idempotent = request.method().equals("GET") || request.method().equals("HEAD")
                || request.method().equals("OPTIONS") || requestOptions.idempotencyKey() != null;
        if (!idempotent) return false;
        return status == -1 || status == 408 || status == 429 || status == 502 || status == 503 || status == 504;
    }

    private static void backoff(int attempt, String retryAfter) {
        long ceiling = retryAfterMillis(retryAfter);
        if (ceiling < 0) ceiling = Math.min(2000L, 200L << Math.min(attempt, 3));
        ceiling = Math.min(30_000L, Math.max(0L, ceiling));
        long millis = ceiling <= 1 ? ceiling : ThreadLocalRandom.current().nextLong(ceiling / 2, ceiling + 1);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new OwlVigilException("owlvigil: retry interrupted", error);
        }
    }

    private static long retryAfterMillis(String value) {
        if (value == null || value.trim().isEmpty()) return -1;
        try {
            return Math.multiplyExact(Long.parseLong(value.trim()), 1000L);
        } catch (ArithmeticException | NumberFormatException ignored) {
            try {
                return Duration.between(ZonedDateTime.now(), ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)).toMillis();
            } catch (Exception invalidDate) {
                return -1;
            }
        }
    }

    private String requireCredential() {
        String value = options.credentialProvider.get();
        if (value == null || value.trim().isEmpty()) throw new IllegalStateException("OwlVigil credential is required");
        return value;
    }

    private static String requestId(Response response) {
        String value = response.header("X-Request-ID");
        if (value == null) value = response.header("Request-ID");
        return value == null ? "" : value;
    }

    private static String readBody(ResponseBody body) throws IOException {
        return body == null ? "" : body.string();
    }

    private static void closeQuietly(ResponseBody body) {
        if (body != null) body.close();
    }

    private static boolean requiresBody(String method) {
        String upper = method.toUpperCase(Locale.ROOT);
        return upper.equals("POST") || upper.equals("PUT") || upper.equals("PATCH");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    @Override
    public void close() {
        if (!options.ownsHttpClient) return;
        options.httpClient.dispatcher().executorService().shutdown();
        options.httpClient.connectionPool().evictAll();
        if (options.httpClient.cache() != null) {
            try {
                options.httpClient.cache().close();
            } catch (IOException ignored) {
                // Closing remains best effort.
            }
        }
    }
}
