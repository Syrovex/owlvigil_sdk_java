# OwlVigil Java SDK Documentation

These guides follow the Go SDK chapter structure while using the Java SDK's actual public API.
These guides target `com.owlvigil:owlvigil-java:0.3.0` and require Java 11 or later.

**Release candidate:** `0.3.0` is the source target for this branch and is awaiting Maven
Central publication. Its dependency examples describe the intended release, not an artifact
that is already available to download. Wait for publication before switching a registry
dependency from `0.2.0` to `0.3.0`.

## Recommended path

New integrations should read chapters 1 through 4 in order. Then go directly to the chapter for
the task at hand.

1. [Quickstart](01-quickstart.md)
2. [Core Concepts](02-concepts.md)
3. [Authentication and Client Configuration](03-authentication-configuration.md)
4. [Model Calls and Streaming](04-gateway.md)
5. [Management and Workspaces](05-management.md)
6. [Members, Access Control, and Gateway Keys](06-management-access-and-keys.md)
7. [Routing, Observability, and Billing](07-management-operations.md)
8. [Webhooks](08-webhooks.md)
9. [Errors and Troubleshooting](09-errors-troubleshooting.md)
10. [API Reference and Example Index](10-reference-examples.md)

## Integration checklist

- Keep Management API Keys and Gateway Keys separate.
- Close `OwlVigilClient` and `SseStream` with try-with-resources.
- Use idempotency keys only for writes explicitly supported by the server contract.
- Retain request IDs, but never log credentials, authorization headers, or sensitive bodies.
- Run `./gradlew clean check` before release.

## Versioning

Version `0.3.0` includes all 144 typed Management operations and their request and response
models. It also fixes SSE event delivery so a complete event does not wait for the following
event or EOF. Version `0.2.0` has only three Management convenience helpers and retains the
SSE prefetch defect. See the [API reference](10-reference-examples.md) for the method index
and [streaming guide](04-gateway.md#streaming) for the upgrade behavior.

