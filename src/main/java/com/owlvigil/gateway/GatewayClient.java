package com.owlvigil.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.owlvigil.core.ApiResponse;
import com.owlvigil.core.RequestOptions;
import com.owlvigil.internal.HttpTransport;
import java.util.Objects;

public final class GatewayClient {
    private final HttpTransport transport;
    private final ObjectMapper mapper;

    public GatewayClient(HttpTransport transport, ObjectMapper mapper) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public ApiResponse<ModelList> listModels() {
        return listModels(RequestOptions.defaults());
    }

    public ApiResponse<ModelList> listModels(RequestOptions options) {
        return transport.execute("GET", "/v1/models", null, ModelList.class, options);
    }

    public ApiResponse<ChatCompletion> createChatCompletion(ChatCompletionRequest request) {
        return createChatCompletion(request, RequestOptions.defaults());
    }

    public ApiResponse<ChatCompletion> createChatCompletion(ChatCompletionRequest request, RequestOptions options) {
        return transport.execute("POST", "/v1/chat/completions", Objects.requireNonNull(request, "request"), ChatCompletion.class, options);
    }

    public ApiResponse<JsonNode> createChatCompletion(JsonNode request, RequestOptions options) {
        return transport.execute("POST", "/v1/chat/completions", Objects.requireNonNull(request, "request"), JsonNode.class, options);
    }

    public SseStream createChatCompletionStream(ChatCompletionRequest request) {
        return createChatCompletionStream(request, RequestOptions.defaults());
    }

    public SseStream createChatCompletionStream(ChatCompletionRequest request, RequestOptions options) {
        return new SseStream(transport.openStream("/v1/chat/completions", Objects.requireNonNull(request, "request").streaming(), options));
    }

    public SseStream createChatCompletionStream(JsonNode request, RequestOptions options) {
        Objects.requireNonNull(request, "request");
        if (!request.isObject()) throw new IllegalArgumentException("request must be a JSON object");
        ObjectNode body = (ObjectNode) request.deepCopy();
        body.put("stream", true);
        return new SseStream(transport.openStream("/v1/chat/completions", body, options));
    }

    public ApiResponse<JsonNode> createResponse(String model, JsonNode input) {
        return createResponse(model, input, RequestOptions.defaults());
    }

    public ApiResponse<JsonNode> createResponse(String model, JsonNode input, RequestOptions options) {
        ObjectNode body = mapper.createObjectNode().put("model", requireModel(model));
        body.set("input", Objects.requireNonNull(input, "input"));
        return transport.execute("POST", "/v1/responses", body, JsonNode.class, options);
    }

    public SseStream createResponseStream(String model, JsonNode input) {
        return createResponseStream(model, input, RequestOptions.defaults());
    }

    public SseStream createResponseStream(String model, JsonNode input, RequestOptions options) {
        ObjectNode body = mapper.createObjectNode().put("model", requireModel(model)).put("stream", true);
        body.set("input", Objects.requireNonNull(input, "input"));
        return new SseStream(transport.openStream("/v1/responses", body, options));
    }

    public ApiResponse<JsonNode> createEmbeddings(String model, JsonNode input) {
        return createEmbeddings(model, input, RequestOptions.defaults());
    }

    public ApiResponse<JsonNode> createEmbeddings(String model, JsonNode input, RequestOptions options) {
        ObjectNode body = mapper.createObjectNode().put("model", requireModel(model));
        body.set("input", Objects.requireNonNull(input, "input"));
        return transport.execute("POST", "/v1/embeddings", body, JsonNode.class, options);
    }

    public ApiResponse<JsonNode> createAnthropicMessage(JsonNode request) {
        return createAnthropicMessage(request, RequestOptions.defaults());
    }

    public ApiResponse<JsonNode> createAnthropicMessage(JsonNode request, RequestOptions options) {
        return transport.execute("POST", "/anthropic/v1/messages", Objects.requireNonNull(request, "request"), JsonNode.class, options);
    }

    private static String requireModel(String model) {
        if (model == null || model.trim().isEmpty()) throw new IllegalArgumentException("model is required");
        return model;
    }
}
