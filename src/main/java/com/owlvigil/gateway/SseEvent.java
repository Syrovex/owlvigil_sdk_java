package com.owlvigil.gateway;

public final class SseEvent {
    private final String event;
    private final String data;
    private final String id;

    public SseEvent(String event, String data, String id) {
        this.event = event == null ? "" : event;
        this.data = data == null ? "" : data;
        this.id = id == null ? "" : id;
    }

    public String event() { return event; }
    public String data() { return data; }
    public String id() { return id; }
}
