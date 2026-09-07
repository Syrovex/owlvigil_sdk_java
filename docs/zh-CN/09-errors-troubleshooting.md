# 错误处理与故障排查

## 错误处理

`OwlVigilApiException` 表示 HTTP API 错误，可读取 `statusCode()`、`code()` 和
`requestId()`；`OwlVigilException` 表示网络、协议、解码或流式读取错误。

```java
try {
    client.gateway().listModels();
} catch (com.owlvigil.errors.OwlVigilApiException error) {
    log.warn("request failed: status={}, request_id={}", error.statusCode(), error.requestId());
}
```

## 按状态码处理

401 检查 Key 类型和状态；403 检查 scope、Workspace 和资源权限；404 检查 ID 与路径；409
先读取当前状态；429、502、503、504 只在安全可重放时退避重试。

## 故障排查

保存 SDK 版本、方法、时间、状态码和 request ID，不保存认证头或敏感正文。流结束后确认
已关闭 `SseStream`。Webhook 验签失败时检查原始 body、Header、secret 和系统时钟。写操作
超时后先查询结果，不要盲目再次提交。
