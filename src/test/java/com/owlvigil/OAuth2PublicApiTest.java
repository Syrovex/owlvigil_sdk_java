package com.owlvigil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.owlvigil.errors.OwlVigilApiException;
import com.owlvigil.errors.OwlVigilException;
import com.owlvigil.oauth2.OAuth2Client.AuthorizationRequest;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

final class OAuth2PublicApiTest {
    @Test
    void authorizationUrlCoversRequiredOptionalAndEncodedParameters() throws Exception {
        try (MockWebServer server = new MockWebServer();
             OwlVigilClient client = OwlVigilClient.builder().oauthBaseUrl(server.url("base/").toString()).build()) {
            var uri = client.oauth2().authorizationUrl(AuthorizationRequest.builder()
                    .clientId("client id").redirectUri("https://example.test/cb?a=1")
                    .scope("openid").scope("profile").state("state value")
                    .codeChallenge("challenge", "S256").parameter("prompt", "consent").build());
            assertThat(uri.getPath()).isEqualTo("/base/oauth/authorize");
            assertThat(uri.getRawQuery()).contains("response_type=code", "client_id=client%20id",
                    "scope=openid%20profile", "state=state%20value", "code_challenge=challenge",
                    "code_challenge_method=S256", "prompt=consent");
            var minimal = client.oauth2().authorizationUrl(AuthorizationRequest.builder()
                    .clientId("c").redirectUri("https://example.test/cb").build());
            assertThat(minimal.getRawQuery()).doesNotContain("scope=", "state=", "code_challenge=");
            assertThatThrownBy(() -> client.oauth2().authorizationUrl(AuthorizationRequest.builder()
                    .clientId(" ").redirectUri("x").build())).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void everyTokenFlowSendsTheExpectedFormAndDecodesTokens() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(token()); server.enqueue(token()); server.enqueue(token()); server.enqueue(tokenWithoutExpiry());
            try (OwlVigilClient client = OwlVigilClient.builder().oauthBaseUrl(server.url("/").toString()).build()) {
                var exchanged = client.oauth2().exchange("c", "s", "code", "https://cb", "verifier");
                assertToken(exchanged);
                assertThat(server.takeRequest().getBody().readUtf8()).contains("grant_type=authorization_code", "client_secret=s", "code_verifier=verifier");
                assertToken(client.oauth2().refresh("c", "s", "refresh"));
                assertThat(server.takeRequest().getPath()).isEqualTo("/oauth/token/refresh");
                assertToken(client.oauth2().clientCredentials("c", "s", List.of("a", "b")));
                assertThat(server.takeRequest().getBody().readUtf8()).contains("grant_type=client_credentials", "scope=a%20b");
                assertThat(client.oauth2().clientCredentials("c", "s", null).expiresAt()).isNull();
                assertThat(server.takeRequest().getBody().readUtf8()).doesNotContain("scope=");
            }
        }
    }

    @Test
    void oauthErrorsAreTypedAndNeverExposeRawBodies() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(400).setBody("{\"error\":\"invalid_grant\",\"error_description\":\"expired\"}"));
            server.enqueue(new MockResponse().setResponseCode(500).setBody("not-json"));
            try (OwlVigilClient client = OwlVigilClient.builder().oauthBaseUrl(server.url("/").toString()).build()) {
                assertThatThrownBy(() -> client.oauth2().refresh("c", null, "r"))
                        .isInstanceOfSatisfying(OwlVigilApiException.class, error -> {
                            assertThat(error.statusCode()).isEqualTo(400);
                            assertThat(error.code()).isEqualTo("invalid_grant");
                            assertThat(error.getMessage()).contains("expired").doesNotContain("refresh_token");
                        });
                assertThatThrownBy(() -> client.oauth2().refresh("c", null, "r"))
                        .isInstanceOf(OwlVigilApiException.class).hasMessageNotContaining("not-json");
            }
            try (OwlVigilClient client = OwlVigilClient.builder().oauthBaseUrl("http://127.0.0.1:1").timeout(java.time.Duration.ofMillis(100)).build()) {
                assertThatThrownBy(() -> client.oauth2().refresh("c", null, "r")).isInstanceOf(OwlVigilException.class);
            }
        }
    }

    private static MockResponse token() {
        return new MockResponse().setHeader("Content-Type", "application/json")
                .setBody("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"token_type\":\"Bearer\",\"expires_in\":60,\"scope\":\"x\"}");
    }
    private static MockResponse tokenWithoutExpiry() { return new MockResponse().setBody("{\"access_token\":\"a\",\"expires_in\":0}"); }
    private static void assertToken(com.owlvigil.oauth2.OAuth2Client.TokenResponse token) {
        assertThat(token.accessToken()).isEqualTo("a"); assertThat(token.refreshToken()).isEqualTo("r");
        assertThat(token.tokenType()).isEqualTo("Bearer"); assertThat(token.expiresIn()).isEqualTo(60);
        assertThat(token.expiresAt()).isNotNull(); assertThat(token.scope()).isEqualTo("x");
    }
}
