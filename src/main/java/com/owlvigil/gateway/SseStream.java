package com.owlvigil.gateway;

import com.owlvigil.errors.OwlVigilException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.NoSuchElementException;
import okhttp3.Response;

public final class SseStream implements AutoCloseable, Iterable<SseEvent> {
    private final Response response;
    private final BufferedReader reader;
    private boolean consumed;
    private boolean eof;
    private String lastEventId = "";

    public SseStream(Response response) {
        this.response = response;
        if (response.body() == null) {
            response.close();
            throw new OwlVigilException("owlvigil: stream response has no body");
        }
        this.reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8));
    }

    @Override
    public Iterator<SseEvent> iterator() {
        if (consumed) throw new IllegalStateException("SSE stream can only be consumed once");
        consumed = true;
        return new Iterator<SseEvent>() {
            private SseEvent next = readEvent();

            @Override public boolean hasNext() { return next != null; }

            @Override public SseEvent next() {
                if (next == null) throw new NoSuchElementException();
                SseEvent current = next;
                next = readEvent();
                return current;
            }
        };
    }

    private SseEvent readEvent() {
        if (eof) return null;
        String event = "";
        StringBuilder data = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    if (data.length() == 0) continue;
                    if (data.charAt(data.length() - 1) == '\n') data.setLength(data.length() - 1);
                    return new SseEvent(event, data.toString(), lastEventId);
                }
                if (line.startsWith(":")) continue;
                int colon = line.indexOf(':');
                String field = colon < 0 ? line : line.substring(0, colon);
                String value = colon < 0 ? "" : line.substring(colon + 1);
                if (value.startsWith(" ")) value = value.substring(1);
                if (field.equals("event")) event = value;
                else if (field.equals("id") && value.indexOf('\0') < 0) lastEventId = value;
                else if (field.equals("data")) data.append(value).append('\n');
            }
            if (data.length() > 0) {
                if (data.charAt(data.length() - 1) == '\n') data.setLength(data.length() - 1);
                eof = true;
                SseEvent result = new SseEvent(event, data.toString(), lastEventId);
                close();
                return result;
            }
            eof = true;
            close();
            return null;
        } catch (IOException error) {
            close();
            throw new OwlVigilException("owlvigil: read SSE stream", error);
        }
    }

    @Override
    public void close() {
        response.close();
    }
}
