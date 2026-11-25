package com.owlvigil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owlvigil.core.RequestOptions;
import com.owlvigil.gateway.ChatCompletionRequest;
import com.owlvigil.gateway.Message;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class SseDeliveryTest {
    static Stream<Arguments> heldOpenFrames() {
        return Stream.of(
                Arguments.of("data: [DONE]\n\n", "", "[DONE]"),
                Arguments.of("event: error\ndata:\n\n", "error", ""),
                Arguments.of("data: {\"choices\":[{\"delta\":{\"content\":\"first\"}}]}\n\n",
                        "", "{\"choices\":[{\"delta\":{\"content\":\"first\"}}]}"));
    }

    @ParameterizedTest
    @MethodSource("heldOpenFrames")
    void deliversTheCurrentEventWithoutWaitingForAnotherEventOrEof(
            String frame, String expectedType, String expectedData) throws Exception {
        var releaseServer = new CountDownLatch(1);
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(frame.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().flush();
            try { releaseServer.await(); }
            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
        try (var client = OwlVigilClient.builder().gatewayKey("fixture-only")
                .gatewayBaseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                .maxRetries(0).build()) {
            var request = ChatCompletionRequest.builder().model("fixture-model")
                    .message(Message.of("user", "fixture-only")).build();
            try (var events = client.gateway().createChatCompletionStream(request,
                    RequestOptions.builder().timeout(Duration.ofSeconds(5)).build())) {
                var iterator = events.iterator();
                assertThat(iterator.hasNext()).isTrue();
                assertThat(iterator.hasNext()).isTrue();
                var event = iterator.next();
                assertThat(event.event()).isEqualTo(expectedType);
                assertThat(event.data()).isEqualTo(expectedData);
                assertThat(releaseServer.getCount()).isEqualTo(1);
            }
        } finally {
            releaseServer.countDown();
            server.stop(0);
        }
    }

    @Test
    void nextWithoutHasNextAndRepeatedEofKeepIteratorContract() throws Exception {
        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setHeader("Content-Type", "text/event-stream")
                    .setBody("data: first\n\ndata: last\n\n"));
            try (var client = OwlVigilClient.builder().gatewayKey("fixture-only")
                    .gatewayBaseUrl(server.url("/").toString()).build();
                    var events = client.gateway().createChatCompletionStream(
                            ChatCompletionRequest.builder().model("fixture-model")
                                    .message(Message.of("user", "fixture-only")).build())) {
                var iterator = events.iterator();
                assertThat(iterator.next().data()).isEqualTo("first");
                assertThat(iterator.next().data()).isEqualTo("last");
                assertThat(iterator.hasNext()).isFalse();
                assertThat(iterator.hasNext()).isFalse();
                assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
            }
        }
    }
}
