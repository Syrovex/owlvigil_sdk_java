package com.owlvigil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlvigil.core.RequestOptions;
import com.owlvigil.gateway.ChatCompletionRequest;
import com.owlvigil.gateway.Message;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

final class GatewayPublicApiTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void everyGatewayOperationUsesItsPublicContract() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(json("{\"object\":\"list\",\"data\":[{\"id\":\"m\",\"object\":\"model\",\"created\":1,\"owned_by\":\"o\"}]}"));
            server.enqueue(json("{\"object\":\"list\",\"data\":[]}"));
            server.enqueue(json(chat()));
            server.enqueue(json(chat()));
            server.enqueue(json("{\"id\":\"raw\"}"));
            server.enqueue(sse());
            server.enqueue(sse());
            server.enqueue(sse());
            server.enqueue(json("{\"id\":\"response-1\"}"));
            server.enqueue(json("{\"id\":\"response-2\"}"));
            server.enqueue(sse());
            server.enqueue(sse());
            server.enqueue(json("{\"data\":[1]}"));
            server.enqueue(json("{\"data\":[2]}"));
            server.enqueue(json("{\"content\":[]}"));
            server.enqueue(json("{\"content\":[]}"));

            try (OwlVigilClient client = client(server)) {
                var modelList = client.gateway().listModels().value();
                assertThat(modelList.object()).isEqualTo("list");
                assertThat(modelList.data()).hasSize(1);
                assertThat(modelList.data().get(0).id()).isEqualTo("m");
                assertThat(modelList.data().get(0).object()).isEqualTo("model");
                assertThat(modelList.data().get(0).created()).isEqualTo(1);
                assertThat(modelList.data().get(0).ownedBy()).isEqualTo("o");
                assertThat(client.gateway().listModels(RequestOptions.defaults()).value().data()).isEmpty();

                ChatCompletionRequest typed = request();
                assertChat(client.gateway().createChatCompletion(typed).value());
                assertChat(client.gateway().createChatCompletion(typed, RequestOptions.defaults()).value());
                assertThat(client.gateway().createChatCompletion(mapper.createObjectNode().put("model", "m"), RequestOptions.defaults()).value().path("id").asText()).isEqualTo("raw");

                try (var stream = client.gateway().createChatCompletionStream(typed)) {
                    assertThat(stream).extracting(event -> event.data()).containsExactly("{\"ok\":true}");
                }
                try (var stream = client.gateway().createChatCompletionStream(typed, RequestOptions.defaults())) {
                    assertThat(stream).hasSize(1);
                }
                try (var stream = client.gateway().createChatCompletionStream(mapper.createObjectNode().put("model", "m"), RequestOptions.defaults())) {
                    assertThat(stream).hasSize(1);
                }

                var input = mapper.createArrayNode().add("hello");
                assertThat(client.gateway().createResponse("m", input).value().path("id").asText()).isEqualTo("response-1");
                assertThat(client.gateway().createResponse("m", input, RequestOptions.defaults()).value().path("id").asText()).isEqualTo("response-2");
                try (var stream = client.gateway().createResponseStream("m", input)) { assertThat(stream).hasSize(1); }
                try (var stream = client.gateway().createResponseStream("m", input, RequestOptions.defaults())) { assertThat(stream).hasSize(1); }
                assertThat(client.gateway().createEmbeddings("m", input).value().path("data")).hasSize(1);
                assertThat(client.gateway().createEmbeddings("m", input, RequestOptions.defaults()).value().path("data")).hasSize(1);
                var anthropic = mapper.createObjectNode().put("model", "m");
                assertThat(client.gateway().createAnthropicMessage(anthropic).value().has("content")).isTrue();
                assertThat(client.gateway().createAnthropicMessage(anthropic, RequestOptions.defaults()).value().has("content")).isTrue();
            }

            List<String> paths = List.of("/v1/models", "/v1/models", "/v1/chat/completions", "/v1/chat/completions",
                    "/v1/chat/completions", "/v1/chat/completions", "/v1/chat/completions", "/v1/chat/completions",
                    "/v1/responses", "/v1/responses", "/v1/responses", "/v1/responses", "/v1/embeddings",
                    "/v1/embeddings", "/anthropic/v1/messages", "/anthropic/v1/messages");
            for (String path : paths) assertThat(server.takeRequest().getPath()).startsWith(path);
        }
    }

    @Test
    void gatewayRejectsEveryInvalidPublicInput() {
        assertThatThrownBy(() -> ChatCompletionRequest.builder().build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatCompletionRequest.builder().model("m").build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatCompletionRequest.builder().model(" ").message(Message.of("u", "x")).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatCompletionRequest.builder().message(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ChatCompletionRequest.builder().messages(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ChatCompletionRequest.builder().temperature(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatCompletionRequest.builder().temperature(-0.1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatCompletionRequest.builder().temperature(2.1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ChatCompletionRequest.builder().maxTokens(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Message.of(null, "x")).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Message.of("user", null)).isInstanceOf(NullPointerException.class);

        try (MockWebServer server = new MockWebServer(); OwlVigilClient client = client(server)) {
            var object = mapper.createObjectNode();
            assertThatThrownBy(() -> client.gateway().createChatCompletion((ChatCompletionRequest) null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> client.gateway().createChatCompletion((com.fasterxml.jackson.databind.JsonNode) null, RequestOptions.defaults())).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> client.gateway().createChatCompletionStream(mapper.createArrayNode(), RequestOptions.defaults())).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> client.gateway().createResponse(" ", object)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> client.gateway().createResponse(null, object)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> client.gateway().createResponse("m", null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> client.gateway().createResponseStream("m", null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> client.gateway().createEmbeddings("m", null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> client.gateway().createAnthropicMessage(null)).isInstanceOf(NullPointerException.class);
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }

    @Test
    void nullableCollectionAndEventAccessorsReturnSafeValues() {
        var models = new com.owlvigil.gateway.ModelList();
        models.data = null;
        assertThat(models.data()).isEmpty();
        var completion = new com.owlvigil.gateway.ChatCompletion();
        completion.choices = null;
        assertThat(completion.choices()).isEmpty();
        var event = new com.owlvigil.gateway.SseEvent(null, null, null);
        assertThat(event.event()).isEmpty(); assertThat(event.data()).isEmpty(); assertThat(event.id()).isEmpty();
    }

    @Test
    void sseParserCoversCommentsFieldsEofAndSingleConsumption() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setHeader("Content-Type", "text/event-stream")
                    .setBody(": comment\nevent: update\nid: one\ndata: first\ndata: second\n\nid: bad\u0000id\nunknown\ndata: eof"));
            try (OwlVigilClient client = client(server); var stream = client.gateway().createChatCompletionStream(request())) {
                var iterator = stream.iterator();
                assertThat(iterator.hasNext()).isTrue();
                var first = iterator.next();
                assertThat(first.event()).isEqualTo("update"); assertThat(first.id()).isEqualTo("one");
                assertThat(first.data()).isEqualTo("first\nsecond");
                assertThat(iterator.next().data()).isEqualTo("eof");
                assertThat(iterator.hasNext()).isFalse();
                assertThatThrownBy(iterator::next).isInstanceOf(java.util.NoSuchElementException.class);
                assertThatThrownBy(stream::iterator).isInstanceOf(IllegalStateException.class);
            }
        }
    }

    private OwlVigilClient client(MockWebServer server) {
        return OwlVigilClient.builder().gatewayKey("test").gatewayBaseUrl(server.url("/").toString()).build();
    }

    private static ChatCompletionRequest request() {
        ChatCompletionRequest value = ChatCompletionRequest.builder().model("m")
                .messages(List.of(Message.of("user", "hello"))).temperature(0).maxTokens(8).build();
        assertThat(value.model()).isEqualTo("m");
        assertThat(value.messages()).hasSize(1);
        assertThat(value.messages().get(0).getRole()).isEqualTo("user");
        assertThat(value.messages().get(0).getContent()).isEqualTo("hello");
        assertThat(value.temperature()).isZero();
        assertThat(value.maxTokens()).isEqualTo(8);
        assertThat(value.stream()).isNull();
        assertThat(value.streaming().stream()).isTrue();
        return value;
    }

    private static void assertChat(com.owlvigil.gateway.ChatCompletion value) {
        assertThat(value.id()).isEqualTo("c");
        assertThat(value.object()).isEqualTo("chat.completion");
        assertThat(value.created()).isEqualTo(1);
        assertThat(value.model()).isEqualTo("m");
        assertThat(value.choices()).hasSize(1);
        assertThat(value.choices().get(0).index()).isZero();
        assertThat(value.choices().get(0).message().role()).isEqualTo("assistant");
        assertThat(value.choices().get(0).message().content()).isEqualTo("ok");
        assertThat(value.choices().get(0).finishReason()).isEqualTo("stop");
        assertThat(value.usage().promptTokens()).isEqualTo(1);
        assertThat(value.usage().completionTokens()).isEqualTo(2);
        assertThat(value.usage().totalTokens()).isEqualTo(3);
    }

    private static String chat() {
        return "{\"id\":\"c\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"m\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"ok\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":2,\"total_tokens\":3}}";
    }

    private static MockResponse json(String body) { return new MockResponse().setHeader("Content-Type", "application/json").setBody(body); }
    private static MockResponse sse() { return new MockResponse().setHeader("Content-Type", "text/event-stream").setBody("data: {\"ok\":true}\n\n"); }
}
