# Authentication and Client Configuration

## Credentials

| Use case | Environment variable | Java configuration |
| --- | --- | --- |
| Model calls | `OWLVIGIL_GATEWAY_KEY` | `gatewayKey` / `gatewayKeyProvider` |
| Management | `OWLVIGIL_API_KEY` | `apiKey` / `apiKeyProvider` |

The credentials are not interchangeable. Use a server-side secret manager and never hard-code
them.

## Client defaults

`OwlVigilClient.builder().build()` uses production URLs, a 60-second timeout, and up to two
retries. Explicit Builder values take precedence over environment variables:

```java
var client = OwlVigilClient.builder()
        .gatewayKeyProvider(() -> secretManager.get("gateway-key"))
        .timeout(java.time.Duration.ofSeconds(30))
        .maxRetries(1)
        .build();
```

## Custom URLs and HTTP client

Configure `gatewayBaseUrl`, `managementBaseUrl`, `oauthBaseUrl`, or provide an external
`okhttp3.OkHttpClient`. Do not let end users control base URLs in production.

## Per-request options

```java
var options = com.owlvigil.core.RequestOptions.builder()
        .timeout(java.time.Duration.ofSeconds(20))
        .maxRetries(0)
        .header("X-Correlation-Id", "job-123")
        .build();
```

The SDK manages Authorization, Host, Content-Type, Content-Length, and Idempotency-Key. Use an
idempotency key only for writes explicitly supported by the server.

