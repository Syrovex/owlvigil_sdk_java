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

```java
try (var stream = client.gateway().createResponseStream("your-model-id",
        new com.fasterxml.jackson.databind.ObjectMapper().getNodeFactory().textNode("Hello"))) {
    for (var event : stream) {
        System.out.println(event.event() + ": " + event.data());
    }
}
```

An `SseStream` can be consumed only once and must be closed. Output received before an
interruption is partial output.

