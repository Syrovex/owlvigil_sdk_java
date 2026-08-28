package com.owlvigil.errors;

public final class OwlVigilApiException extends OwlVigilException {
    private final int statusCode;
    private final String requestId;
    private final String code;

    public OwlVigilApiException(int statusCode, String requestId, String code, String message) {
        super("owlvigil: status=" + statusCode
                + (code == null || code.isEmpty() ? "" : " code=" + code)
                + (requestId == null || requestId.isEmpty() ? "" : " request_id=" + requestId)
                + (message == null || message.isEmpty() ? "" : " message=" + message));
        this.statusCode = statusCode;
        this.requestId = requestId == null ? "" : requestId;
        this.code = code == null ? "" : code;
    }

    public int statusCode() { return statusCode; }
    public String requestId() { return requestId; }
    public String code() { return code; }
}
