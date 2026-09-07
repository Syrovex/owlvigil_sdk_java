# OwlVigil Java SDK Documentation

These guides follow the Go SDK chapter structure while using the Java SDK's actual public API.
The current published artifact is `com.owlvigil:owlvigil-java:0.2.0` and requires Java 11 or later.

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

Maven Central `0.2.0` is the current published version. Source `0.3.0-SNAPSHOT` adds typed
Management methods and models that are not included in `0.2.0`. See
[API reference](10-reference-examples.md) for the complete method index.

