# Webhooks

## Verify inbound signatures

Use the raw request bytes before JSON parsing or reserialization:

```java
byte[] rawBody = requestBodyBytes;
String combined = "t=" + timestampHeader + "," + signatureHeader;
new com.owlvigil.webhook.WebhookVerifier().verify(
        rawBody, combined, System.getenv("OWLVIGIL_WEBHOOK_SECRET"));
```

Read `timestampHeader` from `OwlVigil-Timestamp` and `signatureHeader` from
`OwlVigil-Signature`. The signature header already contains `v1=<hex-hmac>`; do not add
another `v1=`. The combined verifier input is `t=<timestamp>,v1=<hex-hmac>`.
The default tolerance is five minutes. Verification uses HMAC-SHA256 over the timestamp,
a period, and the original raw body bytes. Invalid or stale signatures throw
`IllegalArgumentException`; reject them before parsing JSON or applying side effects.

## Prevent duplicate processing

Parse the verified body and validate its `workspace_id`, `id`, and `type`. Atomically accept
the signed `(workspace_id, id)` pair into durable storage before acknowledging delivery.
The `OwlVigil-Event-Id` and `OwlVigil-Event-Type` headers are not independently signed;
if used, require them to match the verified body.

Automatic retries keep the event ID. Manual redelivery creates a new event ID with copied
payload, so event-ID deduplication alone cannot prevent repeated business work. Make the
business action idempotent using a stable resource or transaction identity.

