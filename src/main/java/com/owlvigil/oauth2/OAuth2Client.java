package com.owlvigil.oauth2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlvigil.errors.OwlVigilApiException;
import com.owlvigil.errors.OwlVigilException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class OAuth2Client {
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final Duration timeout;

    public OAuth2Client(String baseUrl, OkHttpClient httpClient, ObjectMapper mapper, Duration timeout) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.timeout = timeout;
    }

    public URI authorizationUrl(AuthorizationRequest request) {
        Map<String, String> query = new LinkedHashMap<>();
        query.put("response_type", "code");
        query.put("client_id", required(request.clientId, "clientId"));
        query.put("redirect_uri", required(request.redirectUri, "redirectUri"));
        if (!request.scopes.isEmpty()) query.put("scope", String.join(" ", request.scopes));
        if (request.state != null) query.put("state", request.state);
        if (request.codeChallenge != null) query.put("code_challenge", request.codeChallenge);
        if (request.codeChallengeMethod != null) query.put("code_challenge_method", request.codeChallengeMethod);
        query.putAll(request.additionalParameters);
        StringBuilder value = new StringBuilder(baseUrl).append("/oauth/authorize?");
        query.forEach((key, item) -> {
            if (value.charAt(value.length() - 1) != '?') value.append('&');
            value.append(encode(key)).append('=').append(encode(item));
        });
        return URI.create(value.toString());
    }

    public TokenResponse exchange(String clientId, String clientSecret, String code, String redirectUri, String codeVerifier) {
        FormBody.Builder form = new FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", required(clientId, "clientId"))
                .add("code", required(code, "code"))
                .add("redirect_uri", required(redirectUri, "redirectUri"));
        addIfPresent(form, "client_secret", clientSecret);
        addIfPresent(form, "code_verifier", codeVerifier);
        return token("/oauth/token", form.build());
    }

    public TokenResponse refresh(String clientId, String clientSecret, String refreshToken) {
        FormBody.Builder form = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", required(clientId, "clientId"))
                .add("refresh_token", required(refreshToken, "refreshToken"));
        addIfPresent(form, "client_secret", clientSecret);
        return token("/oauth/token/refresh", form.build());
    }

    public TokenResponse clientCredentials(String clientId, String clientSecret, List<String> scopes) {
        FormBody.Builder form = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .add("client_id", required(clientId, "clientId"))
                .add("client_secret", required(clientSecret, "clientSecret"));
        if (scopes != null && !scopes.isEmpty()) form.add("scope", String.join(" ", scopes));
        return token("/oauth/token", form.build());
    }

    private TokenResponse token(String path, FormBody body) {
        Request request = new Request.Builder().url(baseUrl + path).post(body).header("Accept", "application/json").build();
        OkHttpClient client = httpClient.newBuilder().callTimeout(timeout).build();
        try (Response response = client.newCall(request).execute()) {
            String payload = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful()) throw error(response, payload);
            TokenResponse token = mapper.readValue(payload, TokenResponse.class);
            token.expiresAt = token.expiresIn > 0 ? Instant.now().plusSeconds(token.expiresIn) : null;
            return token;
        } catch (IOException error) {
            throw new OwlVigilException("owlvigil: OAuth token request failed", error);
        }
    }

    private OwlVigilApiException error(Response response, String payload) {
        String code = "";
        String message = response.message();
        try {
            JsonNode root = mapper.readTree(payload);
            code = root.path("error").asText("");
            message = root.path("error_description").asText(message);
        } catch (Exception ignored) {
            // Never expose an unparsed OAuth response body.
        }
        return new OwlVigilApiException(response.code(), "", code, message);
    }

    private static void addIfPresent(FormBody.Builder builder, String name, String value) {
        if (value != null && !value.isEmpty()) builder.add(name, value);
    }

    private static String required(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    public static final class AuthorizationRequest {
        private final String clientId;
        private final String redirectUri;
        private final List<String> scopes;
        private final String state;
        private final String codeChallenge;
        private final String codeChallengeMethod;
        private final Map<String, String> additionalParameters;

        private AuthorizationRequest(Builder builder) {
            this.clientId = builder.clientId;
            this.redirectUri = builder.redirectUri;
            this.scopes = Collections.unmodifiableList(new ArrayList<>(builder.scopes));
            this.state = builder.state;
            this.codeChallenge = builder.codeChallenge;
            this.codeChallengeMethod = builder.codeChallengeMethod;
            this.additionalParameters = Collections.unmodifiableMap(new LinkedHashMap<>(builder.additionalParameters));
        }

        public static Builder builder() { return new Builder(); }

        public static final class Builder {
            private String clientId;
            private String redirectUri;
            private final List<String> scopes = new ArrayList<>();
            private String state;
            private String codeChallenge;
            private String codeChallengeMethod;
            private final Map<String, String> additionalParameters = new LinkedHashMap<>();
            public Builder clientId(String value) { clientId = value; return this; }
            public Builder redirectUri(String value) { redirectUri = value; return this; }
            public Builder scope(String value) { scopes.add(value); return this; }
            public Builder state(String value) { state = value; return this; }
            public Builder codeChallenge(String value, String method) { codeChallenge = value; codeChallengeMethod = method; return this; }
            public Builder parameter(String name, String value) { additionalParameters.put(name, value); return this; }
            public AuthorizationRequest build() { return new AuthorizationRequest(this); }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class TokenResponse {
        @JsonProperty("access_token") public String accessToken;
        @JsonProperty("refresh_token") public String refreshToken;
        @JsonProperty("token_type") public String tokenType;
        @JsonProperty("expires_in") public long expiresIn;
        public String scope;
        private Instant expiresAt;
        public String accessToken() { return accessToken; }
        public String refreshToken() { return refreshToken; }
        public String tokenType() { return tokenType; }
        public long expiresIn() { return expiresIn; }
        public Instant expiresAt() { return expiresAt; }
        public String scope() { return scope; }
    }
}
