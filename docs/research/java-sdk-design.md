# OwlVigil Java SDK 设计调研与实施建议

> 调研日期：2026-08-25
> 目标：为 `github.com/Syrovex/owlvigil_sdk_java` 确定第一版可长期维护的架构；本文只给出设计结论，不实现 SDK。
> 主要基线：OpenAI `openai-java` commit [`d7ecd271`](https://github.com/openai/openai-java/tree/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0)。补充对照：Stripe Java [`a87e544f`](https://github.com/stripe/stripe-java/tree/a87e544f21301f7599e924b7092a5bfc48733ba8)、AWS SDK for Java v2 [`9255503c`](https://github.com/aws/aws-sdk-java-v2/tree/9255503c9966fe67e123535ba49f167ac6a45b41)。引用均指向官方文档或官方源码。

## 1. 结论先行

建议第一版采用以下基线：

- **Java 11 运行时下限，JDK 21 构建**，同时在 11、17、21、25 上做消费者兼容测试。Java 11 比 Java 8 更适合一个 2026 年新建的 SDK，又覆盖大量企业存量系统；未来提高运行时下限必须作为主版本变更。
- 使用 **Gradle Wrapper + Kotlin DSL**，发布到 Maven Central；代码本身使用 Java，不要求调用方引入 Kotlin 运行时。
- Maven 坐标建议为 `com.owlvigil:owlvigil-java`，包名为 `com.owlvigil`。公开产物先控制为三个：`owlvigil-java-core`、`owlvigil-java-client-okhttp`、聚合入口 `owlvigil-java`。
- 保留 OwlVigil 的两个业务边界：`GatewayClient` 和 `ManagementClient`，二者共享认证、HTTP、错误、分页与 JSON 运行时，但不合成一个含糊的“万能 client”。
- 每个客户端同时提供 **同步接口和真正的异步接口**：同步返回对象；异步返回 `CompletableFuture<T>`。不能用公共线程池简单包一层阻塞调用。
- 默认传输层采用 **OkHttp**，核心模块只依赖内部 `HttpClient` 抽象，允许以后增加 JDK HTTP Client 或让高级用户注入传输实现。
- 请求/响应模型采用不可变对象和 Builder；Jackson 只属于序列化实现细节。未知响应字段必须保留，未知枚举值不能令整次反序列化失败。
- SSE 同步 API 返回 `AutoCloseable` 流并要求 try-with-resources；异步 API 提供单订阅、可取消、具备完成信号的流对象。解析器遵循 WHATWG SSE 行协议，不能把它当作“一行一个 JSON”。
- 重试默认保守：连接失败、408、429、明确可重试的 5xx；指数退避、抖动并尊重 `Retry-After`。POST 只有在服务端语义安全或传入幂等键时才自动重试；流已经向调用方交付事件后不自动重放。
- 先以 **经校验的 OpenAPI 规范生成模型、服务骨架和契约测试**，手写客户端运行时、SSE、重试和高价值易用层。不要把 OpenAI 的私有 Castiron 生成体系搬进来，也不要在第一版复制其数十个分片模块。

## 2. Java、构建和兼容策略

### 推荐

| 项目 | 建议 |
| --- | --- |
| SDK 源码 | Java；不把 Kotlin 标准库暴露为运行时依赖 |
| 消费者运行时下限 | Java 11 |
| 构建 JDK | JDK 21 LTS，通过 Gradle toolchain 固定 |
| 构建系统 | Gradle Wrapper、Kotlin DSL、dependency locking/verification |
| 测试矩阵 | Java 11、17、21、25；每个发布产物用独立消费者工程做 smoke test |
| 版本语义 | SemVer；提高 JVM 下限、删除公开 API、改变必需传递依赖均为 major |

OpenAI 的框架无关产物仍以 Java 8 为运行时下限，但仓库使用 JDK 21 构建，并通过 `options.release` 保证字节码/API 下限；其政策明确要求“较新的构建 JDK 不得悄悄提高消费者运行时下限”，且把提高 JVM 下限视为 major change（[版本支持政策](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/docs/version-support-policy.md)、[Gradle Java convention](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/buildSrc/src/main/kotlin/openai.java.gradle.kts)、[版本矩阵](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/gradle/version-support.properties)）。OwlVigil 应借鉴“构建 JDK 与运行下限分离”和兼容矩阵，而不是为了表面一致继续背负 Java 8。

Gradle Wrapper 必须提交，插件和 GitHub Actions 固定版本/commit；发布所用 token 与签名材料仅放 CI secret。OpenAI 的安全要求同样明确禁止提交 Maven Central/Sonatype token、GPG 私钥和 API key，并要求审查 wrapper、插件、依赖和发布链路（[CONTRIBUTING：Security expectations](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/CONTRIBUTING.md#security-expectations)）。

## 3. 模块与包结构

### 第一版产物

```text
owlvigil-java-root/
├── owlvigil-java-core/             # 模型、客户端接口、公共错误、HTTP SPI、SSE/分页抽象
├── owlvigil-java-client-okhttp/    # 默认 OkHttp 实现、连接池、TLS/代理
├── owlvigil-java/                  # 聚合产物，只导出 core + okhttp
├── owlvigil-java-example/          # 不发布；可运行示例
└── compatibility-tests/            # 不发布；最小消费者与多 JDK 测试
```

公开包建议：

```text
com.owlvigil.client                 GatewayClient / GatewayClientAsync
                                    ManagementClient / ManagementClientAsync
com.owlvigil.client.okhttp          OwlVigilOkHttpClient 工厂/Builder
com.owlvigil.gateway                Gateway 请求、响应、流事件
com.owlvigil.management             Management 服务与模型
com.owlvigil.oauth2                 OAuth2 模型与服务
com.owlvigil.webhook                Webhook 验签（需要时作为独立小模块）
com.owlvigil.core                   RequestOptions、ResponseMeta、Page、StreamResponse
com.owlvigil.errors                 异常层级
```

OpenAI 将框架无关的 core、OkHttp 客户端和聚合产物分开；聚合产物自身不承载逻辑（[官方项目结构说明](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/CONTRIBUTING.md#project-structure)）。这个边界值得直接借鉴，但其按 API 分片形成的大量 `core-models-*`、`core-services-*` 模块是超大规格和生成流水线的结果，OwlVigil 第一版不应照搬。

不建议第一版发布 Spring Boot starter。普通 Builder、环境变量和依赖注入容器已足够；starter 会把 Spring/JDK 版本矩阵变成 SDK 的长期兼容责任。OpenAI 当前也已将 Spring Boot 2 starter 从活跃构建中退休，并主张不兼容框架代际使用不同坐标（[版本支持政策](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/docs/version-support-policy.md)）。

## 4. 客户端 API：同步、异步与资源所有权

建议公开四个主接口：

```java
GatewayClient gateway = OwlVigilOkHttpClient.builder()
    .gatewayKey(System.getenv("OWLVIGIL_GATEWAY_KEY"))
    .buildGatewayClient();

ManagementClient management = OwlVigilOkHttpClient.builder()
    .apiKey(System.getenv("OWLVIGIL_API_KEY"))
    .buildManagementClient();

GatewayClientAsync async = gateway.async();
```

- 同步方法：`ChatCompletion create(ChatCompletionCreateParams params)`。
- 异步方法：`CompletableFuture<ChatCompletion> create(...)`。
- 每个方法另有带 `RequestOptions` 的重载；不要为每个 header/timeout 产生组合爆炸。
- 客户端不可变、线程安全、可共享，并实现 `AutoCloseable`。README 应明确一个应用通常只创建一个客户端，因为连接池和线程池适合复用；OpenAI 官方 README 也明确给出这一建议（[Client configuration](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/README.md#client-configuration)）。
- `withOptions(UnaryOperator<ClientOptions.Builder>)` 返回共享底层连接池的派生客户端，而不修改原对象；OpenAI 使用同样模式（[Modifying configuration](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/README.md#modifying-configuration)）。
- 明确资源所有权：SDK 自建的 HTTP client 随 SDK client 关闭；用户注入且希望共享的 HTTP client 由用户关闭。AWS 的异步 client builder 对“注入实例”和“传入 builder”采用这一区分（[`SdkAsyncClientBuilder`](https://github.com/aws/aws-sdk-java-v2/blob/9255503c9966fe67e123535ba49f167ac6a45b41/core/sdk-core/src/main/java/software/amazon/awssdk/core/client/builder/SdkAsyncClientBuilder.java)）。

OpenAI 同时公开 `OpenAIClient` / `OpenAIClientAsync`，异步普通方法返回 `CompletableFuture`，并由同一套服务表面提供 sync/async 变体（[README：Asynchronous execution](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/README.md#asynchronous-execution)、[`OpenAIClient`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/client/OpenAIClient.kt)、[`OpenAIClientAsync`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/client/OpenAIClientAsync.kt)）。OwlVigil 可以借鉴公共形态，但应保证异步路径由 OkHttp async call 驱动，而不是 `CompletableFuture.supplyAsync(() -> blockingCall())`。

## 5. 配置、认证与请求级覆盖

Builder 建议支持：

- `gatewayKey(CredentialProvider)` 与 `apiKey(CredentialProvider)` 分开，避免把 Gateway Key 和 Management API Key 混用。
- `baseUrl(URI)`，并提供 Gateway 与 Management 各自的默认生产 URL；拒绝缺 scheme、user-info 或非法 URI。
- `timeout(Timeout)`、`maxRetries(int)`、`httpClient(...)`、`userAgent(...)`、`logLevel(...)`。
- `fromEnv()` 读取 `OWLVIGIL_GATEWAY_KEY`、`OWLVIGIL_API_KEY` 和显式环境/base URL 配置；**显式 Builder 值优先于环境变量**。
- 凭证 Provider 在每次 attempt 前取值，以支持轮换；`toString()`、异常、默认日志永不输出 Authorization、key、token 或请求正文。

请求级 `RequestOptions` 至少包含：总超时/attempt 超时、最大重试、幂等键、额外 header、workspace ID、请求标签。它必须不可变，并可以从 client defaults 派生。Stripe 同时提供客户端级和 `RequestOptions` 级的认证、幂等键、base URL、连接/读取超时和网络重试设置（[`RequestOptions.java`](https://github.com/stripe/stripe-java/blob/a87e544f21301f7599e924b7092a5bfc48733ba8/src/main/java/com/stripe/net/RequestOptions.java)、[README：RequestOptions / retries / timeouts](https://github.com/stripe/stripe-java/blob/a87e544f21301f7599e924b7092a5bfc48733ba8/README.md#configuring-request-options)）。

## 6. HTTP 抽象

`owlvigil-java-core` 可定义窄 SPI：

```java
interface HttpClient extends AutoCloseable {
    HttpResponse execute(HttpRequest request, RequestOptions options);
    CompletableFuture<HttpResponse> executeAsync(HttpRequest request, RequestOptions options);
}
```

内部请求应使用 `URI`、多值 headers、可重复/不可重复 body 标记；响应 body 必须可关闭。中间件顺序建议固定为：

```text
参数校验 → URL/请求构造 → 认证 → 脱敏日志 → retry/timeout → transport → 状态映射 → JSON/SSE 解码
```

OpenAI 的 core `HttpClient` 同时定义 sync/async execute 且继承 `AutoCloseable`（[`HttpClient.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/http/HttpClient.kt)），OkHttp 则位于独立 artifact。值得借鉴的是隔离边界；不建议早期公开过多 transport 内部类型，否则未来修复重试、日志和 body 生命周期时会被二进制兼容绑住。

## 7. 模型与代码生成

### 模型形态

- 请求参数：不可变类 + `builder()`；必填字段在 `build()` 时验证。
- 响应：不可变；列表与 map 防御性复制；时间采用 `Instant`/`OffsetDateTime`，金额不能使用 `double`。
- JSON 命名由 Jackson 注解处理，Java API 保持 idiomatic camelCase。
- 可选字段第一版可采用 nullable getter 或 `Optional` getter，但必须全仓统一；集合返回空集合还是 optional 也必须统一。
- 使用 `@JsonAnySetter/@JsonAnyGetter` 保存未知字段，便于服务端向前扩展。
- 可扩展枚举使用“已知常量 + 原始字符串值”的开放枚举包装，而不是封闭 Java `enum` 直接反序列化。
- 对必填字段建议在访问或显式 `validate()` 时检查，不要因为响应增加未知 enum 值就让整个对象解码失败。

OpenAI 生成模型通过 `JsonField` 区分 missing/null/value、以 `additionalProperties` 保存未知字段，并将严格校验延后到字段访问；其说明明确指出全响应提前校验不利于前向兼容（[`ChatCompletion.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/models/chat/completions/ChatCompletion.kt)、[`ClientOptions.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/ClientOptions.kt)）。OwlVigil 应采用其**前向兼容原则**，但不必第一天复制复杂的 `JsonField/JsonValue` 类型系统；先用 unknown-field map、开放枚举与一致的 null/missing 约定即可。

### 生成策略

1. 将 OwlVigil OpenAPI 文档作为唯一契约源，并在 CI 校验。
2. 生成 endpoint service、request/response model、序列化 metadata 和契约测试。
3. 手写稳定 runtime：client builder、认证、HTTP、retry、SSE、pagination、errors。
4. 生成产物必须可重复；CI 重新生成后 `git diff --exit-code`。
5. 对生成器增加 golden tests，生成区禁止随手手改；扩展点放在明确的手写目录。

OpenAI 官方说明“Most of the SDK is generated code”，使用其内部 Castiron 并配 mock OpenAPI server 测试（[CONTRIBUTING：generation and tests](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/CONTRIBUTING.md#modifying-or-adding-code)）。应借鉴 spec-first 和可重复生成，不应依赖其私有生成器或照抄生成后的 Kotlin 源码。

## 8. SSE 与流式调用

同步 API：

```java
try (StreamResponse<ChatCompletionChunk> response =
         gateway.chat().completions().createStreaming(params)) {
    response.stream().forEach(chunk -> { /* ... */ });
}
```

异步 API 建议返回 `AsyncStreamResponse<T>`，至少提供：

- `subscribe(Handler<T>, Executor)`，只允许订阅一次；
- `completionFuture()`，成功、失败、主动关闭均可观测；
- `close()`/`cancel()`，会取消底层 OkHttp call 并关闭 body；
- 回调串行有序；不得占用 OkHttp dispatcher 线程执行用户的慢回调。

解析必须支持 CR/LF/CRLF、空行派发、注释行、多个 `data:` 拼接、`event`、`id`、`retry` 和 `[DONE]`；先检查初始 HTTP 状态，再处理流内错误事件。OpenAI 的 `SseHandler` 按 WHATWG 算法维护上述字段并合并多行 data（[`SseHandler.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/handlers/SseHandler.kt)）；同步流继承 `AutoCloseable`，异步流有单订阅状态、完成 future 和 close（[`StreamResponse.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/http/StreamResponse.kt)、[`AsyncStreamResponse.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/http/AsyncStreamResponse.kt)）。

第一版不建议直接以 `java.util.concurrent.Flow.Publisher` 作为唯一 API：它会引入完整 Reactive Streams 背压语义，而 SSE/OkHttp body 的真实读取、缓存和取消需要严谨实现。可以先提供简单、明确的异步流对象，未来再添加 adapter。

## 9. 错误、重试与超时

建议异常层级：

```text
OwlVigilException
├── OwlVigilApiException        # status、headers、requestId、error code/type、脱敏 body
│   ├── BadRequestException (400)
│   ├── AuthenticationException (401)
│   ├── PermissionDeniedException (403)
│   ├── NotFoundException (404)
│   ├── ConflictException (409)
│   ├── UnprocessableEntityException (422)
│   ├── RateLimitException (429)
│   └── InternalServerException (5xx)
├── OwlVigilIoException
├── OwlVigilTimeoutException
├── OwlVigilInvalidDataException
└── OwlVigilStreamException
```

错误对象保留 HTTP status、响应 headers、服务端 request ID 与结构化 error，但默认消息/日志不得泄露 credential。OpenAI 按状态码映射 typed exception，并在不能解析错误 body 时仍保留可用异常（[`ErrorHandler.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/handlers/ErrorHandler.kt)）。

重试策略：

- 默认最多 2 次额外尝试；full jitter 指数退避并设上限；尊重 `Retry-After`。
- 可考虑连接错误、408、429、502、503、504；409 只有契约明确可重试时才进入策略。
- GET/HEAD 可按策略重试；POST 只有幂等键或 endpoint 明确保证语义安全时重试。
- 不重试 400/401/403/404/422；不重试反序列化错误；不重试已经开始向用户交付的 SSE。
- 取消和线程 interrupt 必须停止等待及底层 call，并恢复 interrupt flag。
- 注入 `Clock`、Sleeper/DelayStrategy 与随机源，使退避测试不真正 sleep。OpenAI `ClientOptions` 也显式注入 clock/sleeper 用于测试，并默认对连接错误、408、409、429、5xx 重试两次（[`ClientOptions.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/ClientOptions.kt)、[README：Retries](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/README.md#retries)）。OwlVigil 对 POST 应比通用生成器更保守。

超时需拆分：connect、read、write、**单次 attempt**、**整个 API call（含重试退避）**。客户端设置默认值，请求可覆盖。AWS 明确区分 `apiCallAttemptTimeout` 与 `apiCallTimeout`，并允许客户端与请求两级覆盖（[`ClientOverrideConfiguration`](https://github.com/aws/aws-sdk-java-v2/blob/9255503c9966fe67e123535ba49f167ac6a45b41/core/sdk-core/src/main/java/software/amazon/awssdk/core/client/config/ClientOverrideConfiguration.java)）；这比只暴露 OkHttp read timeout 更符合生产系统 deadline 语义。SSE 的 read timeout 应可单独配置或关闭，但必须保留总取消机制。

## 10. 分页

统一抽象：

```java
interface Page<T> {
    List<T> items();
    PageInfo pageInfo();
    boolean hasNextPage();
    Page<T> nextPage();
    Iterable<T> autoPager();
}
```

异步版本返回下一页 `CompletableFuture`，auto-pager 应按需拉取而不是一次性缓存所有页。必须：保留原请求过滤参数、只替换 cursor、检测 `hasMore=true` 但 cursor 为空或 cursor 不前进的异常、避免无限循环，并允许每页访问 response metadata/request ID。OpenAI 的 `AutoPager` 使用惰性的 page sequence，只在 `hasNextPage()` 时取下一页（[`AutoPager.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/AutoPager.kt)、[README：Pagination](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/README.md#pagination)）。

## 11. 测试策略

| 层级 | 必测内容 |
| --- | --- |
| 单元测试 | Builder 校验、URI/header 编码、开放枚举、null/missing、脱敏、错误映射、SSE 状态机、退避计算、分页游标 |
| HTTP 契约测试 | 使用 MockWebServer/WireMock 验证 method/path/query/header/body、response decode、错误 body、request ID、连接关闭 |
| 生成契约 | 针对 OpenAPI mock server 跑每个 operation；重新生成无 diff |
| 异步/并发 | future 完成/异常/取消、线程使用、回调顺序、close race、连接复用、流中途取消 |
| 兼容测试 | 每个发布 artifact 在 Java 11/17/21/25 的独立消费者工程编译并运行 |
| live test | opt-in、只读优先、使用环境变量、日志脱敏；Gateway 与 Management 凭证分开 |

OpenAI 的官方测试栈包含 JUnit 5、Mockito、AssertJ、WireMock，并使用 Prism 对 OpenAPI mock server 做契约测试（[CONTRIBUTING：Running tests](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/CONTRIBUTING.md#running-tests)）。OwlVigil 可采用 JUnit 5 + AssertJ + OkHttp MockWebServer；只有在需要高级故障注入时才加 WireMock，避免测试依赖过重。

关键测试还包括：分块边界恰好落在 UTF-8 多字节字符、CRLF 被拆跨 buffer、多行 data、流内 error、服务端无 `[DONE]`、消费者回调抛异常、取消发生在 headers 前/后、Retry-After 秒数与 HTTP date、POST 无幂等键不得重试。所有 fixture 使用显然虚假的 key。

## 12. 发布与演进

- Maven Central 发布 sources JAR、Javadoc JAR、签名与 SCM/License/Developer POM metadata。
- `0.x` 期间也维护 changelog；到 `1.0.0` 前冻结包名、异常层级、Builder 和 sync/async/stream 形态。
- 使用 release automation 生成 tag、GitHub Release 和 Maven Central deployment；发布任务仅在受保护 tag/environment 可用。
- 用 `japicmp` 或 Revapi 检查二进制兼容；依赖升级配合锁文件、依赖验证和 SBOM。
- 不承诺服务端新增 endpoint 必须 major；新增方法/模型通常 minor，bugfix patch，删除/改签名/JVM floor major。
- 模型对未知字段和枚举保持容忍，减少服务端向前演进迫使 SDK major 的概率。
- README 同时给 Maven/Gradle、同步/异步/SSE、Gateway/Management 两种凭证的最短可运行例子；示例绝不包含真实 key。

OpenAI 按 artifact 声明运行时与生命周期，并要求 public API compatibility 和独立消费者 smoke test（[版本支持政策](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/docs/version-support-policy.md)）。Stripe 的 `StripeClient` 演进则说明：新的显式 client 可以与遗留静态入口并存迁移，但长期维护两套入口会增加负担（[README：StripeClient vs legacy pattern](https://github.com/stripe/stripe-java/blob/a87e544f21301f7599e924b7092a5bfc48733ba8/README.md#stripeclient-vs-legacy-pattern)）；OwlVigil 是新 SDK，应从一开始只提供实例客户端，不提供全局静态 key/client。

## 13. OpenAI 设计：借鉴与不照搬

| 主题 | 借鉴 | 不照搬 |
| --- | --- | --- |
| 模块 | core / OkHttp / 聚合产物三层 | 大量模型与服务分片、Bedrock/Azure 等 OwlVigil 暂不需要的集成 |
| 客户端 | 同步/异步对称、不可变 options、共享 client | 为 API 每个细枝末节生成庞大服务树 |
| HTTP | 核心 transport SPI、资源可关闭 | 过早把全部低层 SPI 承诺为稳定公开 API |
| 模型 | Builder、未知字段、开放枚举、延迟校验 | 第一版复制 `JsonField/JsonValue` 的全部复杂度 |
| SSE | 正确状态机、sync close、async completion/cancel | 以缓存线程池无限承载慢消费者；忽略背压和容量规划 |
| 重试 | typed error、请求级覆盖、可测试 clock/sleeper | 不区分幂等性地重试所有 POST；把 409 一概重试 |
| 生成 | OpenAPI 为契约、生成可重复、mock spec 测试 | 依赖 OpenAI 私有 Castiron 或复制生成源码 |
| 兼容 | build JDK 与 runtime floor 分离、矩阵验证 | 因 OpenAI 历史覆盖面而默认 Java 8 |
| 框架 | framework-neutral core | 第一版即发布 Spring Boot starter/BOM |

## 14. 建议实施顺序

1. 锁定 Java 11/Gradle/JDK 21、包名、Maven 坐标和 OpenAPI 来源。
2. 建 `core`、`client-okhttp`、聚合、example、compatibility-tests 五个 Gradle project。
3. 先完成最小 runtime：options、credentials、HTTP SPI、OkHttp、response metadata、errors、JSON mapper。
4. 打通 Gateway models/list、chat completion、embeddings，以及 SSE；同步/异步同时设计。
5. 打通 Management workspace discovery、usage/log 等只读链路和 cursor pagination，再扩展写操作。
6. 接入 generation drift、契约、跨 JDK、API compatibility、dependency verification CI。
7. 发布 `0.1.0`，用真实 Java 应用验证 API 人体工学与连接/取消行为，再扩大 Management surface。

验收门槛不是“能发 HTTP 请求”，而是：示例可复制运行；sync/async/SSE 均可取消和关闭；凭证不泄露；重试遵守幂等性；未知服务端字段不破坏旧客户端；所有 artifact 在声明的 Java 下限上能被一个干净消费者加载和调用。

## 15. 一手来源索引

- OpenAI Java：[`README.md`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/README.md)、[`CONTRIBUTING.md`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/CONTRIBUTING.md)、[版本支持政策](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/docs/version-support-policy.md)、[`ClientOptions.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/ClientOptions.kt)、[`SseHandler.kt`](https://github.com/openai/openai-java/blob/d7ecd271fa2bf0f85ed56e66d009f89b7aecd2a0/openai-java-core/src/main/kotlin/com/openai/core/handlers/SseHandler.kt)。
- Stripe Java：[`README.md`](https://github.com/stripe/stripe-java/blob/a87e544f21301f7599e924b7092a5bfc48733ba8/README.md)、[`RequestOptions.java`](https://github.com/stripe/stripe-java/blob/a87e544f21301f7599e924b7092a5bfc48733ba8/src/main/java/com/stripe/net/RequestOptions.java)、[`HttpClient.java`](https://github.com/stripe/stripe-java/blob/a87e544f21301f7599e924b7092a5bfc48733ba8/src/main/java/com/stripe/net/HttpClient.java)。
- AWS SDK for Java v2：[`SdkAsyncClientBuilder.java`](https://github.com/aws/aws-sdk-java-v2/blob/9255503c9966fe67e123535ba49f167ac6a45b41/core/sdk-core/src/main/java/software/amazon/awssdk/core/client/builder/SdkAsyncClientBuilder.java)、[`ClientOverrideConfiguration.java`](https://github.com/aws/aws-sdk-java-v2/blob/9255503c9966fe67e123535ba49f167ac6a45b41/core/sdk-core/src/main/java/software/amazon/awssdk/core/client/config/ClientOverrideConfiguration.java)。
