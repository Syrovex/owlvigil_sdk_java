package com.owlvigil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owlvigil.core.ApiResponse;
import com.owlvigil.core.RequestOptions;
import com.owlvigil.core.ResponseMeta;
import com.owlvigil.errors.OwlVigilApiException;
import com.owlvigil.errors.OwlVigilException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

final class ManagementAndCorePublicApiTest {
    @Test
    void everyManagementOperationAndDtoAccessorIsCovered() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            String workspaces = "{\"request_id\":\"r\",\"data\":{\"items\":[{\"id\":1,\"workspace_id\":2,\"name\":\"N\",\"slug\":\"s\",\"status\":\"active\",\"role\":\"owner\"}],\"page_info\":{\"next_cursor\":\"n\",\"has_more\":true}}}";
            server.enqueue(json(workspaces)); server.enqueue(json(workspaces)); server.enqueue(json(workspaces));
            String usage = "{\"data\":{\"workspace_id\":2,\"requests\":3,\"tokens\":4,\"cost\":\"1.25\",\"current_period\":\"month\"}}";
            server.enqueue(json(usage)); server.enqueue(json(usage));
            String log = "{\"data\":{\"request_id\":\"req\",\"model\":\"m\",\"status\":\"ok\",\"status_code\":200,\"total_tokens\":5,\"total_cost\":\"0.5\"}}";
            server.enqueue(json(log)); server.enqueue(json(log));
            try (OwlVigilClient client = OwlVigilClient.builder().apiKey("test").managementBaseUrl(server.url("/").toString()).build()) {
                var first = client.management().listWorkspaces().value();
                var workspace = first.items().get(0);
                assertThat(workspace.id()).isEqualTo(1); assertThat(workspace.workspaceId()).isEqualTo(2);
                assertThat(workspace.name()).isEqualTo("N"); assertThat(workspace.slug()).isEqualTo("s");
                assertThat(workspace.status()).isEqualTo("active"); assertThat(workspace.role()).isEqualTo("owner");
                assertThat(first.pageInfo().nextCursor()).isEqualTo("n"); assertThat(first.pageInfo().hasMore()).isTrue();
                client.management().listWorkspaces("a b", 1);
                client.management().listWorkspaces("", 100, RequestOptions.defaults());
                var summary = client.management().getUsageSummary(2).value();
                assertThat(summary.workspaceId()).isEqualTo(2); assertThat(summary.requests()).isEqualTo(3);
                assertThat(summary.tokens()).isEqualTo(4); assertThat(summary.cost()).isEqualByComparingTo(new BigDecimal("1.25"));
                assertThat(summary.currentPeriod()).isEqualTo("month");
                client.management().getUsageSummary(2, RequestOptions.builder().header("X-Test", "yes").build());
                var requestLog = client.management().getRequestLog(2, "req/a").value();
                assertThat(requestLog.requestId()).isEqualTo("req"); assertThat(requestLog.model()).isEqualTo("m");
                assertThat(requestLog.status()).isEqualTo("ok"); assertThat(requestLog.statusCode()).isEqualTo(200);
                assertThat(requestLog.totalTokens()).isEqualTo(5); assertThat(requestLog.totalCost()).isEqualByComparingTo("0.5");
                client.management().getRequestLog(2, "req", RequestOptions.defaults());
            }
            assertThat(server.takeRequest().getPath()).isEqualTo("/workspaces?limit=50");
            assertThat(server.takeRequest().getPath()).contains("cursor=a%20b");
            assertThat(server.takeRequest().getPath()).isEqualTo("/workspaces?limit=100");
            assertThat(server.takeRequest().getPath()).isEqualTo("/gateway/usage/summary?workspace_id=2");
            assertThat(server.takeRequest().getHeader("X-Test")).isEqualTo("yes");
            assertThat(server.takeRequest().getPath()).contains("req%2Fa");
            assertThat(server.takeRequest().getPath()).contains("workspace_id=2");
        }
    }

    @Test
    void workspaceCollectionsAndPaginationHandleNulls() {
        var list = new com.owlvigil.management.WorkspaceList();
        list.items = null;
        assertThat(list.items()).isEmpty();
        list.pageInfo.nextCursor = null;
        assertThat(list.pageInfo().nextCursor()).isEmpty();
    }

    @Test
    void coreOptionsMetadataAndErrorsExposeTheirContracts() {
        var options = RequestOptions.builder().workspaceId(1).idempotencyKey("i")
                .timeout(Duration.ofSeconds(1)).maxRetries(0).header("X-Test", "v").build();
        assertThat(options.workspaceId()).isEqualTo(1); assertThat(options.idempotencyKey()).isEqualTo("i");
        assertThat(options.timeout()).isEqualTo(Duration.ofSeconds(1)); assertThat(options.maxRetries()).isZero();
        assertThat(options.headers()).containsEntry("X-Test", "v");
        assertThatThrownBy(() -> options.headers().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> RequestOptions.builder().workspaceId(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RequestOptions.builder().idempotencyKey(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RequestOptions.builder().timeout(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RequestOptions.builder().maxRetries(-1)).isInstanceOf(IllegalArgumentException.class);
        for (String header : List.of("Authorization", "Host", "Content-Length", "Content-Type", "Idempotency-Key"))
            assertThatThrownBy(() -> RequestOptions.builder().header(header, "x")).isInstanceOf(IllegalArgumentException.class);

        var meta = new ResponseMeta(201, null, null);
        assertThat(meta.statusCode()).isEqualTo(201); assertThat(meta.requestId()).isEmpty(); assertThat(meta.headers()).isEmpty();
        var response = new ApiResponse<>("ok", new ResponseMeta(200, "r", Map.of("x", List.of("y"))));
        assertThat(response.value()).isEqualTo("ok"); assertThat(response.meta().requestId()).isEqualTo("r");
        assertThat(response.meta().headers()).containsKey("x");
        assertThatThrownBy(() -> new ApiResponse<>(null, meta)).isInstanceOf(NullPointerException.class);

        var api = new OwlVigilApiException(403, "r", "denied", "no");
        assertThat(api.statusCode()).isEqualTo(403); assertThat(api.requestId()).isEqualTo("r"); assertThat(api.code()).isEqualTo("denied");
        assertThat(api.getMessage()).contains("status=403", "request_id=r", "code=denied", "message=no");
        var minimalApi = new OwlVigilApiException(500, null, null, null);
        assertThat(minimalApi.requestId()).isEmpty(); assertThat(minimalApi.code()).isEmpty();
        assertThat(minimalApi.getMessage()).isEqualTo("owlvigil: status=500");
        assertThat(new OwlVigilException("x").getMessage()).isEqualTo("x");
        assertThat(new OwlVigilException("x", api).getCause()).isSameAs(api);
    }

    @Test
    void managementValidationRejectsOutOfRangeValues() throws Exception {
        try (MockWebServer server = new MockWebServer();
             OwlVigilClient client = OwlVigilClient.builder().apiKey("x").managementBaseUrl(server.url("/").toString()).build()) {
            assertThatThrownBy(() -> client.management().listWorkspaces("", 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> client.management().listWorkspaces("", 101)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> client.management().getUsageSummary(0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> client.management().getRequestLog(1, " ")).isInstanceOf(IllegalArgumentException.class);
            server.enqueue(json("{\"data\":{\"items\":[],\"page_info\":{}}}"));
            client.management().listWorkspaces(null, 1, null);
        }
    }

    private static MockResponse json(String body) { return new MockResponse().setHeader("Content-Type", "application/json").setBody(body); }
}
