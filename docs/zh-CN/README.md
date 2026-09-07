# OwlVigil Java SDK 中文文档

这些文档参考 Go SDK 的章节结构，并将示例替换为 Java SDK 的真实 API。当前已发布版本为
`com.owlvigil:owlvigil-java:0.2.0`，要求 Java 11 或更高版本。

## 推荐阅读路径

第一次接入建议按 1～4 章阅读；完成基础接入后直接阅读对应任务章节。

1. [快速开始](01-quickstart.md)
2. [核心概念](02-concepts.md)
3. [认证与客户端配置](03-authentication-configuration.md)
4. [模型调用与流式响应](04-gateway.md)
5. [Management 与工作区](05-management.md)
6. [成员、权限与 Gateway Key](06-management-access-and-keys.md)
7. [路由、可观测性与账单](07-management-operations.md)
8. [Webhook](08-webhooks.md)
9. [错误处理与故障排查](09-errors-troubleshooting.md)
10. [API 参考与示例索引](10-reference-examples.md)

## 接入检查

- 分开保存 Management API Key 与 Gateway Key。
- 使用 try-with-resources 关闭 `OwlVigilClient` 和 `SseStream`。
- 只对服务端明确支持的写操作使用幂等键。
- 记录 request ID，不记录凭证、认证头或敏感正文。
- 上线前运行 `./gradlew clean check`。

## 版本说明

Maven Central 的 `0.2.0` 是当前发布版本；源码中的 `0.3.0-SNAPSHOT` 包含额外的类型化
Management 方法和模型，未包含在 `0.2.0` 中。完整方法索引见
[API reference](10-reference-examples.md)。

