package com.owlvigil.gateway;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public final class Message {
    private final String role;
    private final String content;

    @JsonCreator
    public Message(@JsonProperty("role") String role, @JsonProperty("content") String content) {
        this.role = Objects.requireNonNull(role, "role");
        this.content = Objects.requireNonNull(content, "content");
    }

    public static Message of(String role, String content) { return new Message(role, content); }
    public String role() { return role; }
    public String content() { return content; }
    @JsonProperty("role") public String getRole() { return role; }
    @JsonProperty("content") public String getContent() { return content; }
}
