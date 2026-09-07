# API 参考与示例索引

## API 参考

| Java 类型 | 用途 |
| --- | --- |
| `OwlVigilClient` | 创建并复用 Gateway、Management、OAuth2 客户端 |
| `GatewayClient` | 模型列表、Chat、Responses、Embeddings、Anthropic Messages、SSE |
| `ManagementClient` | 工作区、用量和请求日志便捷方法 |
| `GeneratedManagementClient` | `0.3.0-SNAPSHOT` 的类型化 Management 操作 |
| `RequestOptions` | 单次请求超时、重试、Workspace、幂等键和 Header |
| `WebhookVerifier` | Webhook HMAC-SHA256 验签 |

## 示例索引

- [快速开始](01-quickstart.md)：Maven、Gradle、首次 Chat 调用。
- [模型调用与流式响应](04-gateway.md)：模型发现和 SSE。
- [Management 与工作区](05-management.md)：Workspace、分页、用量和日志。
- [Webhook](08-webhooks.md)：原始 body 验签与去重。
- [错误处理](09-errors-troubleshooting.md)：异常和状态码。

执行 `./gradlew javadocJar` 可生成 Javadoc，其中包含完整的类型化方法索引。
