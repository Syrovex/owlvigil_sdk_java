# 模型调用与流式响应

## Gateway 模型调用

使用 `client.gateway()` 进行模型发现、Chat Completions、Responses、Embeddings 和
Anthropic Messages 调用。

### 发现模型

```java
var models = client.gateway().listModels();
for (var model : models.value().data()) {
    System.out.println(model.id());
}
```

### 普通调用

```java
var result = client.gateway().createChatCompletion(
        com.owlvigil.gateway.ChatCompletionRequest.builder()
                .model("your-model-id")
                .message(com.owlvigil.gateway.Message.of("user", "你好"))
                .build());
```

Responses、Embeddings 和 Anthropic Messages 使用 `JsonNode` overload。复杂输入应按当前
Gateway 契约构造 JSON，不要猜测字段。

## 流式调用

```java
try (var stream = client.gateway().createResponseStream("your-model-id",
        new com.fasterxml.jackson.databind.ObjectMapper().getNodeFactory().textNode("你好"))) {
    for (var event : stream) {
        System.out.println(event.event() + ": " + event.data());
    }
}
```

`SseStream` 只能消费一次，必须关闭。流中断后已收到内容只能视为部分结果。

