package com.owlvigil.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class ChatCompletion {
    public String id;
    public String object;
    public long created;
    public String model;
    public List<Choice> choices = Collections.emptyList();
    public Usage usage;

    public String id() { return id; }
    public String object() { return object; }
    public long created() { return created; }
    public String model() { return model; }
    public List<Choice> choices() { return choices == null ? Collections.emptyList() : Collections.unmodifiableList(choices); }
    public Usage usage() { return usage; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Choice {
        public int index;
        public Message message;
        @JsonProperty("finish_reason") public String finishReason;
        public int index() { return index; }
        public Message message() { return message; }
        public String finishReason() { return finishReason; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Usage {
        @JsonProperty("prompt_tokens") public int promptTokens;
        @JsonProperty("completion_tokens") public int completionTokens;
        @JsonProperty("total_tokens") public int totalTokens;
        public int promptTokens() { return promptTokens; }
        public int completionTokens() { return completionTokens; }
        public int totalTokens() { return totalTokens; }
    }
}
