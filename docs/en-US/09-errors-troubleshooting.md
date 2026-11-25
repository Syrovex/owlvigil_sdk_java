# Errors and Troubleshooting

## Error handling

`OwlVigilApiException` represents an HTTP API error and exposes
`statusCode()`, `code()`, and `requestId()`. `OwlVigilException` represents network,
protocol, decoding, or streaming failures.

```java
try {
    client.gateway().listModels();
} catch (com.owlvigil.errors.OwlVigilApiException error) {
    log.warn("request failed: status={}, request_id={}", error.statusCode(), error.requestId());
}
```

## Handle status codes

For 401 check credential type and status; for 403 check scopes, Workspace access, and resource
permissions; for 404 check IDs and paths; for 409 read current state first. Back off on 429, 502,
503, and 504 only when replay is safe.

## Troubleshooting

Keep the SDK version, method, time, status, and request ID, but never authorization headers or
sensitive bodies. Confirm that every `SseStream` is closed. For failed Webhook verification,
check the raw body, headers, secret, and system clock. After a write timeout, read current state
before submitting again.

### Streaming waits after a complete event

Version `0.2.0` has a one-event prefetch defect: the application may not receive
a delta, error or terminal frame until another frame or EOF arrives. A timeout
can therefore hide an already received terminal or error event. This is fixed
in `0.3.0`, with regression tests that keep the HTTP connection open after
flushing a complete frame. Increasing the timeout in `0.2.0` is not a fix.
See [Streaming](04-gateway.md#streaming) for the upgrade behavior and alternatives
when remaining on `0.2.0`.
