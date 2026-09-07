# Core Concepts

## SDK and API relationship

The Java SDK is an HTTP client. Gateway handles model calls and Management handles
Workspace and control-plane resources. Their credentials and base URLs must not be mixed.

## Client, responses, and request options

Create one reusable, thread-safe `OwlVigilClient`:

```java
try (var client = OwlVigilClient.builder().build()) {
    var response = client.gateway().listModels();
    System.out.println(response.value().data());
}
```

`ApiResponse<T>` contains `value()` and `meta()`. Metadata includes HTTP status,
response headers, and the request ID. `RequestOptions` controls per-request Workspace
ID, timeout, retries, idempotency key, and additional headers.

## Request lifetime

The defaults are a 60-second timeout and up to two retries. Applications should still
choose a business-appropriate per-request timeout. Close every `SseStream` to end a stream.

## Workspaces, pagination, and idempotency

Management resources usually belong to a Workspace. Use the numeric Workspace ID returned by
the service. Cursors are opaque and must be passed back unchanged. Use idempotency keys only
for writes explicitly supported by the server; a network timeout does not prove that a write failed.

## Secrets and ordinary IDs

Gateway Keys, Management API Keys, Provider credentials, OAuth tokens, and Webhook secrets are
secrets. Workspace IDs, model IDs, and request IDs are not secrets, but may still be sensitive.

