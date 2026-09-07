# Webhook

## 验证入站签名

必须使用未经 JSON 解析和重新序列化的原始请求体：

```java
byte[] rawBody = requestBodyBytes;
String combined = "t=" + timestampHeader + ",v1=" + signatureHeader;
new com.owlvigil.webhook.WebhookVerifier().verify(
        rawBody, combined, System.getenv("OWLVIGIL_WEBHOOK_SECRET"));
```

Header 为 `OW-Webhook-Timestamp` 和 `OW-Webhook-Signature`，SDK 需要组合值
`t=<timestamp>,v1=<hex-hmac>`。默认时间容差 5 分钟，算法为 HMAC-SHA256。

## 防止重复处理

验签通过后再解析事件，并使用 `OW-Webhook-Event-Id` 做持久化去重。Webhook 是至少一次
投递，测试、重试和重投都可能产生重复事件。

