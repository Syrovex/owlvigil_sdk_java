package com.owlvigil.management;

import com.owlvigil.core.ApiResponse;
import com.owlvigil.core.RequestOptions;
import com.owlvigil.internal.HttpTransport;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public final class ManagementClient {
    private final HttpTransport transport;

    public ManagementClient(HttpTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public ApiResponse<WorkspaceList> listWorkspaces() {
        return listWorkspaces("", 50, RequestOptions.defaults());
    }

    public ApiResponse<WorkspaceList> listWorkspaces(String cursor, int limit) {
        return listWorkspaces(cursor, limit, RequestOptions.defaults());
    }

    public ApiResponse<WorkspaceList> listWorkspaces(String cursor, int limit, RequestOptions options) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("limit must be between 1 and 100");
        String path = "/workspaces?limit=" + limit;
        if (cursor != null && !cursor.isEmpty()) path += "&cursor=" + encode(cursor);
        return transport.execute("GET", path, null, WorkspaceList.class, options);
    }

    public ApiResponse<UsageSummary> getUsageSummary(long workspaceId) {
        return getUsageSummary(workspaceId, RequestOptions.defaults());
    }

    public ApiResponse<UsageSummary> getUsageSummary(long workspaceId, RequestOptions options) {
        return transport.execute(
                "GET",
                "/gateway/usage/summary",
                null,
                UsageSummary.class,
                withWorkspaceId(workspaceId, options));
    }

    public ApiResponse<RequestLog> getRequestLog(long workspaceId, String requestId) {
        return getRequestLog(workspaceId, requestId, RequestOptions.defaults());
    }

    public ApiResponse<RequestLog> getRequestLog(long workspaceId, String requestId, RequestOptions options) {
        if (requestId == null || requestId.trim().isEmpty()) throw new IllegalArgumentException("requestId is required");
        return transport.execute(
                "GET",
                "/gateway/request-logs/" + encode(requestId),
                null,
                RequestLog.class,
                withWorkspaceId(workspaceId, options));
    }

    private static RequestOptions withWorkspaceId(long workspaceId, RequestOptions options) {
        RequestOptions source = options == null ? RequestOptions.defaults() : options;
        RequestOptions.Builder builder = RequestOptions.builder().workspaceId(workspaceId);
        if (source.idempotencyKey() != null) builder.idempotencyKey(source.idempotencyKey());
        if (source.timeout() != null) builder.timeout(source.timeout());
        if (source.maxRetries() != null) builder.maxRetries(source.maxRetries());
        source.headers().forEach(builder::header);
        return builder.build();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
