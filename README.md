# OwlVigil Java SDK

[![CI](https://github.com/Syrovex/owlvigil_sdk_java/actions/workflows/ci.yml/badge.svg)](https://github.com/Syrovex/owlvigil_sdk_java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.owlvigil/owlvigil-java)](https://central.sonatype.com/artifact/com.owlvigil/owlvigil-java)
[![License: MIT](https://img.shields.io/github/license/Syrovex/owlvigil_sdk_java)](LICENSE)

Java 11+ client for the OwlVigil AI Gateway, Management API, OAuth2, and
webhook signature verification.

Version `0.2.0` is published on Maven Central. The development version
`0.3.0-SNAPSHOT` on `main` adds typed methods and models for all 144
operations in the standalone public Management contract; these additions are
not included in the published `0.2.0` artifact. See
[CHANGELOG.md](CHANGELOG.md) for details.

## Features

- Gateway models, Chat Completions, Responses, Embeddings, and Anthropic Messages
- SSE streaming for Chat Completions and Responses
- Workspace listing, usage summary, and request-log lookup
- OAuth2 authorization and token flows
- Webhook HMAC signing and verification

## Installation

Add the published `0.2.0` package from Maven Central:

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

## Quick start

Set `OWLVIGIL_GATEWAY_KEY` for model calls. Model names are provider-facing
names such as `deepseek-v4-flash`, not internal model IDs.

```java
import com.owlvigil.OwlVigilClient;
import com.owlvigil.gateway.ChatCompletionRequest;
import com.owlvigil.gateway.Message;

try (OwlVigilClient client = OwlVigilClient.builder().build()) {
    var response = client.gateway().createChatCompletion(
        ChatCompletionRequest.builder()
            .model("deepseek-v4-flash")
            .message(Message.of("user", "Hello"))
            .build());
    System.out.println(response.value().choices().get(0).message().content());
}
```

`build()` does not require both credentials immediately. The SDK reads each
credential when its API surface sends a request, so an application that only
uses the Gateway needs only `OWLVIGIL_GATEWAY_KEY`.

Use `OWLVIGIL_API_KEY` only for Management calls. Discover the workspace ID
through `client.management().listWorkspaces()` before calling workspace-scoped
methods. Never use the Management key as a Gateway key.

```java
try (OwlVigilClient client = OwlVigilClient.builder().build()) {
    var workspaces = client.management().listWorkspaces().value().items();
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

Streaming methods return an `SseStream`; always close it with
try-with-resources. Requests support per-call timeout, retry, idempotency key,
and safe custom headers through `RequestOptions`. Retries are conservative and
limited to idempotent requests or requests carrying an idempotency key.

## Configuration

The builder defaults to the production endpoints, a 60-second timeout, two
retries, and an `owlvigil-java/<SDK version>` user agent. Explicit builder
values take precedence over environment variables.

```java
OwlVigilClient client = OwlVigilClient.builder()
        .gatewayKey("replace-with-gateway-key")
        .apiKey("replace-with-management-key")
        .timeout(java.time.Duration.ofSeconds(30))
        .maxRetries(1)
        .build();
```

Do not hard-code real credentials. Use environment variables or a
`gatewayKeyProvider` / `apiKeyProvider` backed by your server-side secret
manager.

## Development

```bash
./gradlew clean check jar sourcesJar javadocJar
```

`check` includes the public API coverage gate. The full JaCoCo report is written
to `build/reports/jacoco/test/html/index.html`.

Live tests are opt-in and read-only:

```bash
OWLVIGIL_LIVE_TEST=1 \
OWLVIGIL_GATEWAY_KEY='replace-with-gateway-key' \
OWLVIGIL_API_KEY='replace-with-management-key' \
./gradlew test --rerun-tasks
```

## Documentation

- Developer guides: [English](docs/DEVELOPER_GUIDE.en-US.md) · [简体中文](docs/DEVELOPER_GUIDE.zh-CN.md)
- [CHANGELOG.md](CHANGELOG.md)

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md)
before opening a pull request, and follow the
[Code of Conduct](CODE_OF_CONDUCT.md).

Report security vulnerabilities privately through the process in
[SECURITY.md](SECURITY.md). For usage questions and bug reports, see
[SUPPORT.md](SUPPORT.md).

## License

Licensed under the [MIT License](LICENSE).
