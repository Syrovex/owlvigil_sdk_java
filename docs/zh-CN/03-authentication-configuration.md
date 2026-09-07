# 认证与客户端配置

## 认证与凭证

| 场景 | 环境变量 | Java 配置 |
| --- | --- | --- |
| 模型调用 | `OWLVIGIL_GATEWAY_KEY` | `gatewayKey` / `gatewayKeyProvider` |
| Management 管理 | `OWLVIGIL_API_KEY` | `apiKey` / `apiKeyProvider` |

两类 Key 不能互换。生产环境使用密钥管理服务，不要硬编码凭证。

## 客户端默认值

`OwlVigilClient.builder().build()` 默认使用生产地址、60 秒超时和最多 2 次重试。明确的
Builder 配置优先于环境变量：

```java
var client = OwlVigilClient.builder()
        .gatewayKeyProvider(() -> secretManager.get("gateway-key"))
        .timeout(java.time.Duration.ofSeconds(30))
        .maxRetries(1)
        .build();
```

## 自定义地址与 HTTP Client

可配置 `gatewayBaseUrl`、`managementBaseUrl`、`oauthBaseUrl`，以及外部
`okhttp3.OkHttpClient`。生产系统不要允许终端用户控制 Base URL。

## 单次请求选项

```java
var options = com.owlvigil.core.RequestOptions.builder()
        .timeout(java.time.Duration.ofSeconds(20))
        .maxRetries(0)
        .header("X-Correlation-Id", "job-123")
        .build();
```

SDK 管理认证、Host、Content-Type、Content-Length 和 Idempotency-Key，不能通过附加 Header
覆盖。幂等键只用于服务端明确支持的写操作。

