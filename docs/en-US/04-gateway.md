# Model Calls and Streaming

## Gateway model calls

Use `client.gateway()` for model discovery, Chat Completions, Responses, Embeddings, and
Anthropic Messages.

### Discover models

```java
var models = client.gateway().listModels();
for (var model : models.value().data()) {
    System.out.println(model.id());
}
```

### Regular calls

```java
var result = client.gateway().createChatCompletion(
        com.owlvigil.gateway.ChatCompletionRequest.builder()
                .model("your-model-id")
                .message(com.owlvigil.gateway.Message.of("user", "Hello"))
                .build());
```

Responses, Embeddings, and Anthropic Messages use `JsonNode` overloads. Build complex input
according to the current Gateway contract rather than guessing fields.

## Streaming

**Version boundary:** `0.3.0` fixes delayed SSE event delivery. Version `0.2.0`
reads one event ahead, so a complete delta, named error or terminal frame may
wait for another event or EOF; a connection left open can time out before the
application receives that frame. Upgrade to `0.3.0` for the corrected behavior.
If you remain on `0.2.0`, use non-streaming calls or an independently tested
direct HTTP SSE implementation. A longer request deadline does not repair
event delivery in `0.2.0`.

```java
try (var stream = client.gateway().createResponseStream("your-model-id",
        new com.fasterxml.jackson.databind.ObjectMapper().getNodeFactory().textNode("Hello"))) {
    for (var event : stream) {
        if ("error".equals(event.event())) {
            throw new IllegalStateException("Gateway reported a streaming error");
        }
        if ("[DONE]".equals(event.data().trim())) break;
        if (event.data().trim().isEmpty()) continue;
        System.out.println(event.event() + ": " + event.data());
    }
}
```

An `SseStream` can be consumed only once and must be closed. Output received before an
interruption is partial output.

Starting with `0.3.0`, requesting an iterator does not read from the connection.
`hasNext()` reads at most the next complete event and caches it; repeated calls
do not consume another event. `next()` returns that event without prefetching
the following one. It also works without a preceding `hasNext()` call. An
attempt to read a new event can still block until data, EOF or the configured
request deadline. The SDK exposes SSE frames as strings; applications own
event-specific JSON parsing, terminal handling and error detection. Check named
errors before empty or terminal data and close the stream on every exit.

