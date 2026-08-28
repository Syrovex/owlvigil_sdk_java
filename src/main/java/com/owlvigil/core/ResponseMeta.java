package com.owlvigil.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ResponseMeta {
    private final int statusCode;
    private final String requestId;
    private final Map<String, List<String>> headers;

    public ResponseMeta(int statusCode, String requestId, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.requestId = requestId == null ? "" : requestId;
        this.headers = headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers);
    }

    public int statusCode() { return statusCode; }
    public String requestId() { return requestId; }
    public Map<String, List<String>> headers() { return headers; }
}
