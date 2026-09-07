package com.owlvigil.core;

import java.util.Objects;

public final class ApiResponse<T> {
    private final T value;
    private final ResponseMeta meta;

    public ApiResponse(T value, ResponseMeta meta) {
        this.meta = Objects.requireNonNull(meta, "meta");
        this.value = meta.statusCode() == 204 ? value : Objects.requireNonNull(value, "value");
    }

    /** Returns the decoded value, or null for HTTP 204 No Content. */
    public T value() { return value; }
    public ResponseMeta meta() { return meta; }
}
