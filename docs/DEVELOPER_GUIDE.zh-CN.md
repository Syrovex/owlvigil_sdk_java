# OwlVigil Java SDK 开发者文档

> **SDK：** `com.owlvigil:owlvigil-java` · **Java：** 11+ · **已发布版本：** `0.2.0`

本文按照 Go SDK 的开发者文档组织，但示例全部使用 Java SDK 当前公开 API。Java SDK
通过 HTTP 访问 OwlVigil，不连接 Dashboard 数据库、Redis 或其他服务端基础设施。

## 1. 接入前先理解两类 API

OwlVigil 将模型调用和管理操作分成两个 API 面：

| API 面 | 默认地址 | 凭证 | 用途 |
| --- | --- | --- | --- |
| Gateway Runtime | `https://gateway.owlvigil.com` | Gateway Key | 模型列表、Chat、Responses、Embeddings、Anthropic Messages |
| Management API | `https://api.owlvigil.com/v1` | Management API Key | 工作区、用量、请求日志及管理资源 |

两类 Key 不能互换。环境变量 `OWLVIGIL_GATEWAY_KEY` 只用于
`client.gateway()`；`OWLVIGIL_API_KEY` 只用于 `client.management()`。

## 2. 安装与第一次调用

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

把 Gateway Key 放到服务端环境变量，不要写入源码、日志或前端代码：

```bash
export OWLVIGIL_GATEWAY_KEY='your-gateway-key'
```

最小 Chat Completions 调用：

```java
import com.owlvigil.OwlVigilClient;
import com.owlvigil.gateway.ChatCompletionRequest;
import com.owlvigil.gateway.Message;

try (OwlVigilClient client = OwlVigilClient.builder().build()) {
    var response = client.gateway().createChatCompletion(
            ChatCompletionRequest.builder()
                    .model("deepseek-v4-flash")
                    .message(Message.of("user", "用一句话介绍 OwlVigil。"))
                    .maxTokens(64)
                    .build());

    System.out.println(response.value().choices().get(0).message().content());
    System.out.println("request_id=" + response.meta().requestId());
}
```

模型名取决于当前 Workspace、Provider、路由和 Gateway Key。生产代码应先调用
`client.gateway().listModels()`，不要假设示例模型一定可用。

## 3. 客户端与认证配置

### 默认值

`OwlVigilClient.builder().build()` 使用以下配置：

| 配置 | 默认值 |
| --- | --- |
| Gateway Key | `OWLVIGIL_GATEWAY_KEY` |
| Management API Key | `OWLVIGIL_API_KEY` |
| Gateway Base URL | `https://gateway.owlvigil.com` |
| Management Base URL | `https://api.owlvigil.com/v1` |
| OAuth Base URL | `https://open.owlvigil.com` |
| 超时 | 60 秒 |
| 最大重试次数 | 2 |
| User-Agent | `owlvigil-java/<SDK version>` |

客户端构造时不会立即要求两类 Key；只有对应 API 发起请求时才读取并校验对应凭证。
因此只调用 Gateway 时不需要配置 Management Key。

### 显式配置和密钥提供器

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

生产环境可使用 `gatewayKeyProvider` 和 `apiKeyProvider` 对接密钥管理服务：

```java
var client = OwlVigilClient.builder()
        .gatewayKeyProvider(() -> secretManager.get("owlvigil/gateway-key"))
        .build();
```

也可以设置 `gatewayBaseUrl`、`managementBaseUrl`、`oauthBaseUrl`，用于 staging、
本地测试或私有部署。不要让普通终端用户控制这些地址，否则可能导致凭证泄露或 SSRF。

一个 `OwlVigilClient` 可安全复用。SDK 自己创建的客户端必须关闭；使用外部
`OkHttpClient` 时，连接池的所有权仍属于调用方。

## 4. Gateway 模型调用

### 模型发现

```java
try (var client = OwlVigilClient.builder().build()) {
    var models = client.gateway().listModels();
    for (var model : models.value().data()) {
        System.out.println(model.id() + " owned_by=" + model.ownedBy());
    }
}
```

### Chat Completions

`ChatCompletionRequest` 对常见文本请求提供类型化 Builder：

```java
var request = ChatCompletionRequest.builder()
        .model("your-model-id")
        .message(Message.of("system", "回答要简洁。"))
        .message(Message.of("user", "解释什么是幂等请求。"))
        .temperature(0.2)
        .maxTokens(256)
        .build();

var result = client.gateway().createChatCompletion(request);
var choices = result.value().choices();
if (choices.isEmpty() || choices.get(0).message() == null) {
    throw new IllegalStateException("empty response: " + result.meta().requestId());
}
```

`temperature` 的范围是 0 到 2，`maxTokens` 必须为正数。多模态、工具调用和
Provider-specific 字段使用 Jackson `JsonNode` overload，以保持向前兼容。

### Responses、Embeddings 和 Anthropic Messages

这些接口接收 `JsonNode`，SDK 不替应用决定服务端的结构化输入：

```java
import com.fasterxml.jackson.databind.ObjectMapper;

var json = new ObjectMapper();
var input = json.createArrayNode().add("第一段文本").add("第二段文本");

var response = client.gateway().createResponse("your-model-id", input);
var embeddings = client.gateway().createEmbeddings("embedding-model", input);

var anthropicRequest = json.createObjectNode()
        .put("model", "your-model-id")
        .put("max_tokens", 256)
        .set("messages", json.createArrayNode());
var message = client.gateway().createAnthropicMessage(anthropicRequest);
```

实际字段和返回结构以当前 Gateway API 契约为准；不要把示例中的模型名、向量维度
或输出字段当作固定值。

### SSE 流式响应

Chat 和 Responses 的流式方法返回只能消费一次、必须关闭的 `SseStream`：

```java
var request = ChatCompletionRequest.builder()
        .model("your-model-id")
        .message(Message.of("user", "从一数到三。"))
        .build()
        .streaming();

try (var stream = client.gateway().createChatCompletionStream(request)) {
    for (var event : stream) {
        System.out.println("event=" + event.event() + " data=" + event.data());
    }
}
```

流事件的 `data` 是未按业务解码的字符串。消费结束或发生异常都要关闭 Stream；
网络中断后的已展示内容只能视为部分结果。

## 5. Management 与工作区

Management API Key 的环境变量：

```bash
export OWLVIGIL_API_KEY='your-management-api-key'
```

先列出当前凭证可访问的工作区，再使用返回的数字 ID：

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

不要从 slug 猜测 ID，也不要默认把列表第一项当成生产工作区；生产系统应使用已验证的
租户映射或显式配置。

`listWorkspaces()` 默认请求 50 条；需要分页时使用
`listWorkspaces(cursor, limit)`，其中 `limit` 必须为 1 到 100，cursor 只能原样传回。
当前已发布 Java SDK 的便捷 Management 方法还包括 `getUsageSummary` 和
`getRequestLog`。

源码中的 `0.3.0-SNAPSHOT` 另外生成了覆盖公开 Management 合约的类型化方法和模型。
这些方法尚未包含在 Maven Central 的 `0.2.0` 中；使用前请确认依赖版本和
[CHANGELOG.md](https://github.com/Syrovex/owlvigil_sdk_java/blob/main/CHANGELOG.md)
中的版本说明。

## 6. 单次请求选项、返回值与错误

### RequestOptions

每个 Gateway 和 Management 请求都可以传入 `RequestOptions` overload：

```java
import com.owlvigil.core.RequestOptions;
import java.time.Duration;

var options = RequestOptions.builder()
        .workspaceId(16)
        .timeout(Duration.ofSeconds(20))
        .maxRetries(1)
        .idempotencyKey("gateway-key-create-production-v1")
        .header("X-Correlation-Id", "job-123")
        .build();
```

SDK 会管理 `Authorization`、`Host`、`Content-Type`、`Content-Length` 和
`Idempotency-Key`，这些 Header 不能通过 `header` 覆盖。幂等键只应发送给服务端明确
支持的写操作；写请求超时后不能仅凭网络错误判断服务端未执行。

### ApiResponse 和 Request ID

成功调用返回 `ApiResponse<T>`：

```java
var response = client.gateway().listModels();
var value = response.value();
int status = response.meta().statusCode();
String requestId = response.meta().requestId();
```

HTTP 204 时 `value()` 可以为 `null`。排障时保存 request ID、方法、路径、状态码和
UTC 时间，但不要保存认证头、完整 prompt 或敏感响应正文。

### 异常类型和状态码

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
    // 网络、协议、解码或流式读取失败。
}
```

| 状态 | 建议 |
| --- | --- |
| 400 / 422 | 修正参数，不重试 |
| 401 | 检查 Key 类型、值、状态和过期时间 |
| 403 | 检查 scope、Workspace 关系和资源权限 |
| 404 | 检查资源 ID、路径和 Workspace |
| 409 | 先读取当前状态，再决定是否写入 |
| 429 | 遵循服务端等待提示并退避 |
| 502 / 503 / 504 | 仅在请求可安全重放时重试 |

SDK 默认只对 GET、HEAD、OPTIONS 以及带幂等键的请求进行自动重试，并针对
408、429、502、503、504 和可重试的传输错误处理。业务层已有重试策略时，应把
单次请求的 `maxRetries(0)` 设为 0，避免重试叠加。

## 7. OAuth2

`client.oauth2()` 提供授权地址和 Token 交换：

```java
var authorization = client.oauth2().authorizationUrl(
        com.owlvigil.oauth2.OAuth2Client.AuthorizationRequest.builder()
                .clientId("client-id")
                .redirectUri("https://example.com/callback")
                .scope("openid")
                .state("csrf-state")
                .build());
```

随后可使用 `exchange`、`refresh` 和 `clientCredentials`。生产授权码流程必须校验
回调中的 `state`，并优先使用 PKCE；Token 只能存放在服务端安全存储中。

## 8. Webhook 验签

验签必须使用 HTTP 请求体的原始字节，不要先 JSON decode 再重新序列化：

```java
import com.owlvigil.webhook.WebhookVerifier;

byte[] rawBody = requestBodyBytes;
String signature = "t=" + timestampHeader + ",v1=" + signatureHeader;

new WebhookVerifier().verify(
        rawBody,
        signature,
        System.getenv("OWLVIGIL_WEBHOOK_SECRET"));
```

OwlVigil 将时间戳和签名分别放在 `OW-Webhook-Timestamp`、
`OW-Webhook-Signature` Header 中；传给 SDK 前组合为
`t=<unix-seconds>,v1=<hex-hmac>`。默认时间容差为 5 分钟，算法为 HMAC-SHA256。
验签通过后再解析和处理事件，并以事件 ID 做持久化去重，因为 Webhook 是至少一次投递。

## 9. 安全接入检查

- 两类 API Key 分开保存、分开轮换，最小化 scope 和 Workspace 权限。
- Gateway Key 或 Webhook secret 只在创建/轮换响应中保存一次时立即写入密钥管理服务。
- 不记录 Authorization、完整 Key、Provider credential、Webhook secret 或敏感正文。
- 创建、删除、支付、轮换和重投前确认环境、Workspace、资源 ID 与副作用。
- 所有 SSE Stream 使用 try-with-resources；所有关键响应保存 request ID。

## 10. 本地验证与参考

在 Java SDK 仓库执行：

```bash
./gradlew clean check jar sourcesJar javadocJar
```

`check` 包含公共 API 覆盖率门禁；Live 测试默认关闭，只有明确配置凭证并设置
`OWLVIGIL_LIVE_TEST=1` 时才运行。执行 `./gradlew javadocJar` 可生成 Javadoc，
其中包含类型化 Management API 与完整方法索引。

HTTP 资源、scope、状态码与公共 envelope 以
[OwlVigil Developer Center](https://github.com/Syrovex/owlvigil_openapi/blob/main/docs/owlvigil-developer-center.md)
和 OpenAPI 规范为准。

