# OwlVigil Java SDK

[![CI](https://github.com/Syrovex/owlvigil_sdk_java/actions/workflows/ci.yml/badge.svg)](https://github.com/Syrovex/owlvigil_sdk_java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.owlvigil/owlvigil-java)](https://central.sonatype.com/artifact/com.owlvigil/owlvigil-java)
[![License: MIT](https://img.shields.io/github/license/Syrovex/owlvigil_sdk_java)](LICENSE)

Java 11+ SDK for OwlVigil Gateway model calls, Management API workflows,
OAuth2, and Webhook signature verification.

Documentation: [here](docs/en-US/README.md)

Gateway model calls use `https://gateway.owlvigil.com` with a Gateway Key.
Management calls use `https://api.owlvigil.com/v1` with a scoped Management
API Key. These credentials are not interchangeable.

## Install

Add the published package from Maven Central:

```kotlin
implementation("com.owlvigil:owlvigil-java:0.2.0")
```

```xml
<dependency>
  <groupId>com.owlvigil</groupId>
  <artifactId>owlvigil-java</artifactId>
  <version>0.2.0</version>
</dependency>
```

The SDK requires Java 11 or later.

## Run a Gateway request

Set the Gateway Key in the server environment:

```bash
export OWLVIGIL_GATEWAY_KEY='your-gateway-key'
```

Then create and reuse one client:

```java
import com.owlvigil.OwlVigilClient;
import com.owlvigil.gateway.ChatCompletionRequest;
import com.owlvigil.gateway.Message;

try (OwlVigilClient client = OwlVigilClient.builder().build()) {
    var response = client.gateway().createChatCompletion(
            ChatCompletionRequest.builder()
                    .model("your-model-id")
                    .message(Message.of("user", "Say hello from OwlVigil."))
                    .build());

    if (response.value().choices().isEmpty()) {
        throw new IllegalStateException("empty response");
    }
    System.out.println(response.value().choices().get(0).message().content());
    System.out.println("request_id=" + response.meta().requestId());
}
```

Use `client.gateway().listModels()` first to discover models available to the
current Workspace and Gateway Key. Model names are provider-facing names, not
internal model IDs.

## Run a Management request

Set a separate Management API Key:

```bash
export OWLVIGIL_API_KEY='your-management-api-key'
```

List Workspaces before calling workspace-scoped methods:

```java
try (OwlVigilClient client = OwlVigilClient.builder().build()) {
    var workspaces = client.management().listWorkspaces().value().items();
    for (var workspace : workspaces) {
        System.out.println(workspace.id() + " " + workspace.name());
    }

    if (!workspaces.isEmpty()) {
        var workspace = workspaces.get(0);
        long workspaceId = workspace.workspaceId() > 0
                ? workspace.workspaceId()
                : workspace.id();
        var usage = client.management().getUsageSummary(workspaceId);
        System.out.println(usage.value().requests());
    }
}
```

Do not use the Management API Key as a Gateway Key.

## Configuration

The builder defaults to the production endpoints, a 60-second timeout, two
retries, and an `owlvigil-java/<SDK version>` User-Agent. Explicit builder
values take precedence over environment variables.

```java
var client = OwlVigilClient.builder()
        .gatewayKeyProvider(() -> secretManager.get("owlvigil/gateway-key"))
        .timeout(java.time.Duration.ofSeconds(30))
        .maxRetries(1)
        .build();
```

For staging, local tests, or private deployments, configure
`gatewayBaseUrl`, `managementBaseUrl`, and `oauthBaseUrl`. Do not let
ordinary end users control these URLs.

Streaming methods return a closeable, single-use `SseStream`. Per-request
timeout, retry, idempotency key, Workspace ID, and safe custom headers are
configured with `RequestOptions`.

## Supported surfaces

- Gateway models, Chat Completions, Responses, Embeddings, and Anthropic Messages
- SSE streaming for Chat Completions and Responses
- Workspace listing, usage summary, and request-log lookup
- Typed Management operations in the development `0.3.0-SNAPSHOT`
- OAuth2 authorization-code, refresh-token, and client-credentials flows
- Webhook HMAC signing and verification

The published `0.2.0` artifact does not include the additional typed
Management operations from `0.3.0-SNAPSHOT`. See [CHANGELOG.md](CHANGELOG.md)
and the [API reference](docs/en-US/10-reference-examples.md) before selecting
a version.

## Verify

```bash
./gradlew clean check jar sourcesJar javadocJar
```

`check` includes the public API coverage gate. Live tests are opt-in and
read-only:

```bash
OWLVIGIL_LIVE_TEST=1 \
OWLVIGIL_GATEWAY_KEY='replace-with-gateway-key' \
OWLVIGIL_API_KEY='replace-with-management-key' \
./gradlew test --rerun-tasks
```

The SDK is HTTP-only. It does not import Dashboard or backend code and does not
connect directly to databases, Redis, queues, or provider infrastructure.

## Community and security

- Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.
- Report vulnerabilities using [SECURITY.md](SECURITY.md).
- See [SUPPORT.md](SUPPORT.md) for usage questions and bug reports.
- Releases and compatibility changes are documented in [CHANGELOG.md](CHANGELOG.md).

Licensed under the [MIT License](LICENSE).
