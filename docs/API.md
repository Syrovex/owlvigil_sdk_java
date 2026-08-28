# API overview

Create one thread-safe `OwlVigilClient` and reuse it. The client exposes
`gateway()`, `management()`, and `oauth2()` surfaces.

## Gateway

- `listModels`
- `createChatCompletion` and `createChatCompletionStream`
- `createResponse` and `createResponseStream`
- `createEmbeddings`
- `createAnthropicMessage`

Typed Chat requests cover the common text case. The `JsonNode` overload accepts
forward-compatible multimodal, tool, and provider-specific fields. Streaming
returns a single-use, closeable `SseStream`.

## Management

- `listWorkspaces`
- `getUsageSummary`
- `getRequestLog`

Use the numeric workspace ID returned by `listWorkspaces`; do not invent or
parse an ID from a slug. Management responses unwrap the OwlVigil `data`
envelope while preserving request metadata.

## Request and error behavior

`RequestOptions` configures workspace ID, idempotency key, timeout, retry limit,
and additional non-reserved headers. Failures use `OwlVigilApiException` for
HTTP API errors and `OwlVigilException` for transport, protocol, and streaming
errors. Log request IDs, never credentials or sensitive bodies.

OAuth2 helpers build authorization URLs and exchange or refresh tokens.
`WebhookVerifier` verifies signed payload bytes before business processing.
