# Webhooks

## Verify inbound signatures

Use the raw request bytes before JSON parsing or reserialization:

```java
byte[] rawBody = requestBodyBytes;
String combined = "t=" + timestampHeader + ",v1=" + signatureHeader;
new com.owlvigil.webhook.WebhookVerifier().verify(
        rawBody, combined, System.getenv("OWLVIGIL_WEBHOOK_SECRET"));
```

The headers are `OW-Webhook-Timestamp` and `OW-Webhook-Signature`. Combine them as
`t=<timestamp>,v1=<hex-hmac>`. The default tolerance is five minutes and the algorithm is
HMAC-SHA256.

## Prevent duplicate processing

Parse events only after verification and deduplicate persistently by
`OW-Webhook-Event-Id`. Delivery is at least once, so tests, retries, and redeliveries may repeat
an event.

