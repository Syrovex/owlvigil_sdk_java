# API Reference and Example Index

## API reference

| Java type | Purpose |
| --- | --- |
| `OwlVigilClient` | Creates and reuses Gateway, Management, and OAuth2 clients |
| `GatewayClient` | Models, Chat, Responses, Embeddings, Anthropic Messages, and SSE |
| `ManagementClient` | Convenience methods for Workspaces, usage, and request logs |
| `GeneratedManagementClient` | Typed Management operations in `0.3.0-SNAPSHOT` |
| `RequestOptions` | Per-request timeout, retries, Workspace, idempotency, and headers |
| `WebhookVerifier` | HMAC-SHA256 Webhook verification |

## Example index

- [Quickstart](01-quickstart.md): Maven, Gradle, and the first Chat request.
- [Model Calls and Streaming](04-gateway.md): model discovery and SSE.
- [Management and Workspaces](05-management.md): Workspaces, pagination, usage, and logs.
- [Webhooks](08-webhooks.md): raw-body verification and deduplication.
- [Errors](09-errors-troubleshooting.md): exceptions and status codes.

Run `./gradlew javadocJar` for the generated Javadoc, which lists every typed
Management method.
