# OwlVigil Java SDK

Java 11+ client for the OwlVigil AI Gateway, Management API, OAuth2, and
webhook signature verification. Version 0.1.x is a preview and implements a
focused Management API surface rather than every Dashboard operation.

## Installation

After version 0.1.0 is published to Maven Central:

```kotlin
implementation("com.owlvigil:owlvigil-java:0.1.0")
```

```xml
<dependency>
  <groupId>com.owlvigil</groupId>
  <artifactId>owlvigil-java</artifactId>
  <version>0.1.0</version>
</dependency>
```

Until then, use `./gradlew publishToMavenLocal` from a trusted source checkout.

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

Use `OWLVIGIL_API_KEY` only for Management calls. Discover the workspace ID
through `client.management().listWorkspaces()` before calling workspace-scoped
methods. Never use the Management key as a Gateway key.

Streaming methods return an `SseStream`; always close it with
try-with-resources. Requests support per-call timeout, retry, idempotency key,
and safe custom headers through `RequestOptions`. Retries are conservative and
limited to idempotent requests or requests carrying an idempotency key.

## Supported preview surface

- Gateway models, Chat Completions, Responses, Embeddings, and Anthropic Messages
- SSE streaming for Chat Completions and Responses
- Workspace listing, usage summary, and request-log lookup
- OAuth2 authorization and token flows
- Webhook HMAC signing and verification

## Development

```bash
./gradlew clean test jar sourcesJar javadocJar
```

Live tests are opt-in and read-only. See the [API overview](docs/API.md), [CONTRIBUTING.md](CONTRIBUTING.md),
[SECURITY.md](SECURITY.md), [SUPPORT.md](SUPPORT.md), and
[CHANGELOG.md](CHANGELOG.md). Licensed under the [MIT License](LICENSE).
