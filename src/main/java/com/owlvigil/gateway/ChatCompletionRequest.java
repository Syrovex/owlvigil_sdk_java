package com.owlvigil.gateway;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ChatCompletionRequest {
    private final String model;
    private final List<Message> messages;
    private final Double temperature;
    private final Integer maxTokens;
    private final Boolean stream;

    private ChatCompletionRequest(Builder builder) {
        if (builder.model == null || builder.model.trim().isEmpty()) throw new IllegalArgumentException("model is required");
        if (builder.messages.isEmpty()) throw new IllegalArgumentException("messages must not be empty");
        this.model = builder.model;
        this.messages = Collections.unmodifiableList(new ArrayList<>(builder.messages));
        this.temperature = builder.temperature;
        this.maxTokens = builder.maxTokens;
        this.stream = builder.stream;
    }

    public static Builder builder() { return new Builder(); }
    @JsonProperty("model") public String model() { return model; }
    @JsonProperty("messages") public List<Message> messages() { return messages; }
    @JsonProperty("temperature") public Double temperature() { return temperature; }
    @JsonProperty("max_tokens") public Integer maxTokens() { return maxTokens; }
    @JsonProperty("stream") public Boolean stream() { return stream; }

    public ChatCompletionRequest streaming() {
        Builder builder = builder().model(model).messages(messages).stream(true);
        if (temperature != null) builder.temperature(temperature);
        if (maxTokens != null) builder.maxTokens(maxTokens);
        return builder.build();
    }

    public static final class Builder {
        private String model;
        private final List<Message> messages = new ArrayList<>();
        private Double temperature;
        private Integer maxTokens;
        private Boolean stream;

        public Builder model(String value) { this.model = value; return this; }
        public Builder message(Message value) { this.messages.add(Objects.requireNonNull(value, "message")); return this; }
        public Builder messages(List<Message> values) {
            Objects.requireNonNull(values, "messages").forEach(this::message);
            return this;
        }
        public Builder temperature(double value) {
            if (!Double.isFinite(value) || value < 0 || value > 2) throw new IllegalArgumentException("temperature must be between 0 and 2");
            this.temperature = value;
            return this;
        }
        public Builder maxTokens(int value) {
            if (value <= 0) throw new IllegalArgumentException("maxTokens must be positive");
            this.maxTokens = value;
            return this;
        }
        Builder stream(boolean value) { this.stream = value; return this; }
        public ChatCompletionRequest build() { return new ChatCompletionRequest(this); }
    }
}
