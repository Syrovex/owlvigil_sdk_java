package com.owlvigil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owlvigil.core.RequestOptions;
import com.owlvigil.errors.OwlVigilApiException;
import com.owlvigil.gateway.ChatCompletionRequest;
import com.owlvigil.gateway.Message;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.OkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

final class OwlVigilClientTest {
    @Test
    void publicVersionMatchesBuildVersion() {
        assertThat(OwlVigilClient.VERSION).isEqualTo(System.getProperty("owlvigil.project.version"));
    }

    @Test
    void listsModelsAndCreatesChatCompletion() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setHeader("X-Request-ID", "req_models")
                    .setBody("{\"object\":\"list\",\"data\":[{\"id\":\"deepseek-v4-flash\"}]}"));
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setHeader("X-Request-ID", "req_chat")
                    .setBody("{\"id\":\"chat_1\",\"model\":\"deepseek-v4-flash\",\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\",\"content\":\"hello\"}}]}"));

            try (OwlVigilClient client = OwlVigilClient.builder()
                    .gatewayKey("test_gateway_key")
                    .gatewayBaseUrl(server.url("/").toString())
                    .timeout(Duration.ofSeconds(5))
                    .build()) {
                assertThat(client.gateway().listModels().value().data().get(0).id())
                        .isEqualTo("deepseek-v4-flash");

                var response = client.gateway().createChatCompletion(ChatCompletionRequest.builder()
                        .model("deepseek-v4-flash")
                        .message(Message.of("user", "hello"))
                        .build());
                assertThat(response.value().choices().get(0).message().content()).isEqualTo("hello");
                assertThat(response.meta().requestId()).isEqualTo("req_chat");
            }

            assertThat(server.takeRequest().getHeader("Authorization")).isEqualTo("Bearer test_gateway_key");
            assertThat(server.takeRequest().getPath()).isEqualTo("/v1/chat/completions");
        }
    }

    @Test
    void preservesManagementErrorRequestIdFromEnvelope() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setResponseCode(403)
                    .setHeader("Content-Type", "application/json")
                    .setBody("{\"code\":\"denied\",\"message\":\"no access\",\"request_id\":\"req_body\"}"));
            try (OwlVigilClient client = OwlVigilClient.builder()
                    .apiKey("test_api_key")
                    .managementBaseUrl(server.url("/").toString())
                    .build()) {
                assertThatThrownBy(() -> client.management().listWorkspaces())
                        .isInstanceOfSatisfying(OwlVigilApiException.class,
                                error -> assertThat(error.requestId()).isEqualTo("req_body"));
            }
        }
    }

    @Test
    void rejectsAuthenticationHeaderOverride() {
        assertThatThrownBy(() -> RequestOptions.builder().header("AuThOrIzAtIoN", "Bearer wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("managed by the SDK");
    }

    @Test
    void builderCoversEveryConfigurationOptionAndValidationBranch() throws Exception {
        assertThatThrownBy(() -> OwlVigilClient.builder().gatewayKeyProvider(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().apiKeyProvider(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().gatewayBaseUrl(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().managementBaseUrl(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().oauthBaseUrl("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().timeout(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().timeout(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().userAgent(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().objectMapper(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OwlVigilClient.builder().httpClient(null)).isInstanceOf(NullPointerException.class);
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("{\"object\":\"list\",\"data\":[]}"));
            OkHttpClient http = new OkHttpClient();
            try (OwlVigilClient client = OwlVigilClient.builder()
                    .gatewayKeyProvider(() -> "provider-key").apiKeyProvider(() -> "api-key")
                    .gatewayBaseUrl(server.url("/").toString()).managementBaseUrl(server.url("/").toString())
                    .oauthBaseUrl(server.url("/").toString()).timeout(Duration.ofSeconds(2)).maxRetries(0)
                    .userAgent("custom-agent").objectMapper(new ObjectMapper()).httpClient(http).build()) {
                client.gateway().listModels();
            }
            assertThat(server.takeRequest().getHeader("User-Agent")).isEqualTo("custom-agent");
            assertThat(http.dispatcher().executorService().isShutdown()).isFalse();
            http.dispatcher().executorService().shutdown();
            http.connectionPool().evictAll();
        }
        try (OwlVigilClient client = OwlVigilClient.builder().gatewayKey(" ").build()) {
            assertThatThrownBy(() -> client.gateway().listModels()).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void retainsSseEventId() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody("id: event-1\ndata: one\n\ndata: two\n\n"));
            try (OwlVigilClient client = OwlVigilClient.builder()
                    .gatewayKey("test_gateway_key")
                    .gatewayBaseUrl(server.url("/").toString())
                    .build();
                    var stream = client.gateway().createChatCompletionStream(ChatCompletionRequest.builder()
                            .model("deepseek-v4-flash")
                            .message(Message.of("user", "hello"))
                            .build())) {
                assertThat(stream).extracting(event -> event.id()).containsExactly("event-1", "event-1");
            }
        }
    }
}
