package com.owlvigil.core;

import java.util.Objects;

public final class ApiResponse<T> {
    private final T value;
    private final ResponseMeta meta;

    public ApiResponse(T value, ResponseMeta meta) {
        this.value = Objects.requireNonNull(value, "value");
        this.meta = Objects.requireNonNull(meta, "meta");
    }

    public T value() { return value; }
    public ResponseMeta meta() { return meta; }
}
