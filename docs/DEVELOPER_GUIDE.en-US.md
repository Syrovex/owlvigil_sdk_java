# OwlVigil Java SDK Developer Guide

> **SDK:** `com.owlvigil:owlvigil-java` · **Java:** 11+ · **Published version:** `0.2.0`

This guide follows the structure of the Go SDK documentation while using the Java
SDK's actual public API. The Java SDK communicates with OwlVigil over HTTP. It
does not connect to Dashboard databases, Redis, or other server-side
infrastructure.

## 1. Understand the two API surfaces

OwlVigil separates model inference from management operations:

| API surface | Default URL | Credential | Use it for |
| --- | --- | --- | --- |
| Gateway Runtime | `https://gateway.owlvigil.com` | Gateway Key | Models, Chat, Responses, Embeddings, and Anthropic Messages |
| Management API | `https://api.owlvigil.com/v1` | Management API Key | Workspaces, usage, request logs, and management resources |

The two credentials are not interchangeable. Use `OWLVIGIL_GATEWAY_KEY` only
with `client.gateway()`, and `OWLVIGIL_API_KEY` only with
`client.management()`.

## 2. Install and make your first request

### Maven

```xml
<dependency>
  <groupId>com.owlvigil</groupId>
  <artifactId>owlvigil-java</artifactId>
  <version>0.2.0</version>
</dependency>
```

### Gradle Kotlin DSL

```kotlin
implementation("com.owlvigil:owlvigil-java:0.2.0")
```

Keep the Gateway Key in a server-side environment variable:

```bash
export OWLVIGIL_GATEWAY_KEY='your-gateway-key'
```

Minimal Chat Completions request:

```java
import com.owlvigil.OwlVigilClient;
import com.owlvigil.gateway.ChatCompletionRequest;
import com.owlvigil.gateway.Message;

try (OwlVigilClient client = OwlVigilClient.builder().build()) {
    var response = client.gateway().createChatCompletion(
            ChatCompletionRequest.builder()
                    .model("deepseek-v4-flash")
                    .message(Message.of("user", "Describe OwlVigil in one sentence."))
                    .maxTokens(64)
                    .build());

    System.out.println(response.value().choices().get(0).message().content());
    System.out.println("request_id=" + response.meta().requestId());
}
```

Available model names depend on the Workspace, Providers, routes, and Gateway
Key. Production applications should call `client.gateway().listModels()`
first instead of assuming that the example model is available.

## 3. Client and authentication configuration

### Defaults

`OwlVigilClient.builder().build()` uses:

| Setting | Default |
| --- | --- |
| Gateway Key | `OWLVIGIL_GATEWAY_KEY` |
| Management API Key | `OWLVIGIL_API_KEY` |
| Gateway base URL | `https://gateway.owlvigil.com` |
| Management base URL | `https://api.owlvigil.com/v1` |
| OAuth base URL | `https://open.owlvigil.com` |
| Timeout | 60 seconds |
| Maximum retries | 2 |
| User-Agent | `owlvigil-java/<SDK version>` |

Client construction does not immediately require both credentials. Each
credential is read and validated when its corresponding API surface sends a
request. An application that only uses Gateway therefore does not need a
Management Key.

### Explicit values and secret providers

```java
import java.time.Duration;
import com.owlvigil.OwlVigilClient;

OwlVigilClient client = OwlVigilClient.builder()
        .gatewayKey(System.getenv("OWLVIGIL_GATEWAY_KEY"))
        .apiKey(System.getenv("OWLVIGIL_API_KEY"))
        .timeout(Duration.ofSeconds(30))
        .maxRetries(1)
        .build();
```

For production, use `gatewayKeyProvider` or `apiKeyProvider` to read from a
secret manager:

```java
var client = OwlVigilClient.builder()
        .gatewayKeyProvider(() -> secretManager.get("owlvigil/gateway-key"))
        .build();
```

You can also set `gatewayBaseUrl`, `managementBaseUrl`, and
`oauthBaseUrl` for staging, local tests, or private deployments. Do not let
ordinary end users control these URLs.

Reuse one `OwlVigilClient` instance. Close the client when the SDK created its
HTTP client. If you supply an external `OkHttpClient`, its connection-pool
ownership remains with your application.

## 4. Gateway model calls

### Discover models

```java
try (var client = OwlVigilClient.builder().build()) {
    var models = client.gateway().listModels();
    for (var model : models.value().data()) {
        System.out.println(model.id() + " owned_by=" + model.ownedBy());
    }
}
```

### Chat Completions

`ChatCompletionRequest` provides a typed builder for common text requests:

```java
var request = ChatCompletionRequest.builder()
        .model("your-model-id")
        .message(Message.of("system", "Be concise."))
        .message(Message.of("user", "Explain idempotent requests."))
        .temperature(0.2)
        .maxTokens(256)
        .build();

var result = client.gateway().createChatCompletion(request);
var choices = result.value().choices();
if (choices.isEmpty() || choices.get(0).message() == null) {
    throw new IllegalStateException("empty response: " + result.meta().requestId());
}
```

`temperature` must be between 0 and 2, and `maxTokens` must be positive.
Use the Jackson `JsonNode` overload for multimodal input, tool calls, and
provider-specific fields.

### Responses, Embeddings, and Anthropic Messages

These methods accept `JsonNode`, allowing the application to provide the
structure supported by the server:

```java
import com.fasterxml.jackson.databind.ObjectMapper;

var json = new ObjectMapper();
var input = json.createArrayNode().add("First document").add("Second document");

var response = client.gateway().createResponse("your-model-id", input);
var embeddings = client.gateway().createEmbeddings("embedding-model", input);

var anthropicRequest = json.createObjectNode()
        .put("model", "your-model-id")
        .put("max_tokens", 256)
        .set("messages", json.createArrayNode());
var message = client.gateway().createAnthropicMessage(anthropicRequest);
```

Use the current Gateway contract for request and response fields. Do not treat
example model names, vector dimensions, or output fields as fixed values.

### SSE streaming

Chat and Responses streaming methods return an `SseStream` that can be
consumed only once and must be closed:

```java
var request = ChatCompletionRequest.builder()
        .model("your-model-id")
        .message(Message.of("user", "Count from one to three."))
        .build()
        .streaming();

try (var stream = client.gateway().createChatCompletionStream(request)) {
    for (var event : stream) {
        System.out.println("event=" + event.event() + " data=" + event.data());
    }
}
```

The event `data` is an undecoded string. Close the stream on both normal and
exceptional paths. Output received before a network interruption is partial
output.

## 5. Management and Workspaces

Set the Management API Key in the server environment:

```bash
export OWLVIGIL_API_KEY='your-management-api-key'
```

List accessible Workspaces first, then use the numeric ID returned by the
service:

```java
try (var client = OwlVigilClient.builder().build()) {
    var page = client.management().listWorkspaces();
    for (var workspace : page.value().items()) {
        System.out.println(workspace.id() + " " + workspace.name());
    }

    if (!page.value().items().isEmpty()) {
        var workspace = page.value().items().get(0);
        long workspaceId = workspace.workspaceId() > 0
                ? workspace.workspaceId()
                : workspace.id();
        var usage = client.management().getUsageSummary(workspaceId);
        System.out.println("requests=" + usage.value().requests());
    }
}
```

Do not infer an ID from a slug or assume the first listed Workspace is the
production Workspace. Use a verified tenant mapping or explicit configuration.

`listWorkspaces()` requests 50 items by default. Use
`listWorkspaces(cursor, limit)` for pagination; `limit` must be between 1
and 100, and a cursor must be passed back unchanged. The published SDK also
provides the convenience methods `getUsageSummary` and `getRequestLog`.

The source `0.3.0-SNAPSHOT` additionally generates typed methods and models
for the public Management contract. These additions are not part of the
published Maven Central `0.2.0` artifact. See
[CHANGELOG.md](https://github.com/Syrovex/owlvigil_sdk_java/blob/main/CHANGELOG.md)
for version boundaries.

## 6. Per-request options, responses, and errors

### RequestOptions

Gateway and Management methods provide overloads accepting `RequestOptions`:

```java
import com.owlvigil.core.RequestOptions;
import java.time.Duration;

var options = RequestOptions.builder()
        .workspaceId(16)
        .timeout(Duration.ofSeconds(20))
        .maxRetries(1)
        .idempotencyKey("operation-key-v1")
        .header("X-Correlation-Id", "job-123")
        .build();
```

The SDK manages `Authorization`, `Host`, `Content-Type`,
`Content-Length`, and `Idempotency-Key`; these headers cannot be overridden.
Use an idempotency key only when the server contract explicitly supports it.
A network timeout does not prove that a write was not processed.

### ApiResponse and request IDs

Successful calls return `ApiResponse<T>`:

```java
var response = client.gateway().listModels();
var value = response.value();
int status = response.meta().statusCode();
String requestId = response.meta().requestId();
```

For HTTP 204, `value()` may be `null`. Store the request ID, method, path,
status, and UTC timestamp for troubleshooting, but never store credentials or
sensitive request and response bodies.

### Exceptions and status codes

```java
import com.owlvigil.errors.OwlVigilApiException;
import com.owlvigil.errors.OwlVigilException;

try {
    client.gateway().listModels();
} catch (OwlVigilApiException error) {
    System.err.println("status=" + error.statusCode()
            + " code=" + error.code()
            + " request_id=" + error.requestId());
} catch (OwlVigilException error) {
    // Network, protocol, decoding, or streaming failure.
}
```

| Status | Recommended action |
| --- | --- |
| 400 / 422 | Fix the request; do not retry |
| 401 | Check credential type, value, status, and expiry |
| 403 | Check scope, Workspace access, and resource permissions |
| 404 | Check the resource ID, path, and Workspace |
| 409 | Read current state before deciding whether to write again |
| 429 | Follow the server's wait guidance and back off |
| 502 / 503 / 504 | Retry only when replay is safe |

The SDK retries only idempotent HTTP methods (GET, HEAD, and OPTIONS), or a
request carrying an idempotency key. It handles 408, 429, 502, 503, 504, and
retryable transport failures. If the application already has a retry policy,
set per-request `maxRetries(0)` to avoid multiplying retries.

## 7. OAuth2

`client.oauth2()` builds authorization URLs and exchanges tokens:

```java
var authorization = client.oauth2().authorizationUrl(
        com.owlvigil.oauth2.OAuth2Client.AuthorizationRequest.builder()
                .clientId("client-id")
                .redirectUri("https://example.com/callback")
                .scope("openid")
                .state("csrf-state")
                .build());
```

Use `exchange`, `refresh`, and `clientCredentials` for token operations.
Authorization-code applications must validate the returned `state` and should
use PKCE. Store tokens in a server-side secret store.

## 8. Webhook verification

Verify the raw HTTP request bytes before JSON parsing or reserialization:

```java
import com.owlvigil.webhook.WebhookVerifier;

byte[] rawBody = requestBodyBytes;
String signature = "t=" + timestampHeader + ",v1=" + signatureHeader;

new WebhookVerifier().verify(
        rawBody,
        signature,
        System.getenv("OWLVIGIL_WEBHOOK_SECRET"));
```

OwlVigil sends the timestamp and signature in the
`OW-Webhook-Timestamp` and `OW-Webhook-Signature` headers. Combine them as
`t=<unix-seconds>,v1=<hex-hmac>` before passing the value to the SDK. The
default tolerance is five minutes and the algorithm is HMAC-SHA256.

After verification, parse and process the event. Deduplicate by event ID in
durable storage because webhook delivery is at least once.

## 9. Security checklist

- Keep Management API Keys and Gateway Keys separate; use least privilege.
- Store Gateway Key and webhook secrets immediately in a secret manager when
  they are returned by a create or rotate operation.
- Never log Authorization headers, complete credentials, Provider credentials,
  webhook secrets, or sensitive bodies.
- Confirm the environment, Workspace, resource ID, and side effects before
  deletes, payments, rotations, and redeliveries.
- Always close SSE streams with try-with-resources and retain request IDs for
  important responses.

## 10. Validation and references

From the Java SDK repository, run:

```bash
./gradlew clean check jar sourcesJar javadocJar
```

`check` includes the public API coverage gate. Live tests are disabled by
default and run only when credentials are configured with
`OWLVIGIL_LIVE_TEST=1`. Run `./gradlew javadocJar` for the generated Javadoc,
which covers the typed Management API and complete method index.

For HTTP resources, scopes, status codes, and the public response envelope,
see the [OwlVigil Developer Center](https://github.com/Syrovex/owlvigil_openapi/blob/main/docs/owlvigil-developer-center.md)
and the OpenAPI specification.

