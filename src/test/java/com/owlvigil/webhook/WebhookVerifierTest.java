package com.owlvigil.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

final class WebhookVerifierTest {
    @Test
    void verifiesSignedPayload() {
        byte[] payload = "{\"type\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        long timestamp = 1_700_000_000L;
        String header = WebhookVerifier.signHeader(payload, timestamp, "whsec_test");
        WebhookVerifier verifier = new WebhookVerifier(
                Duration.ofMinutes(5),
                Clock.fixed(Instant.ofEpochSecond(timestamp), ZoneOffset.UTC));

        assertThatCode(() -> verifier.verify(payload, header, "whsec_test")).doesNotThrowAnyException();
    }

    @Test
    void rejectsEveryInvalidSignatureShapeAndTimestamp() {
        byte[] payload = "{}".getBytes(StandardCharsets.UTF_8);
        long now = 1_700_000_000L;
        WebhookVerifier verifier = new WebhookVerifier(Duration.ofMinutes(5), Clock.fixed(Instant.ofEpochSecond(now), ZoneOffset.UTC));
        assertThatThrownBy(() -> verifier.verify(payload, null, "s")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verify(payload, "x", " ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verify(payload, "bad", "s")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verify(payload, "t=no,v1=aa", "s")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verify(payload, "t=" + now + ",v1=a", "s")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verify(payload, "t=" + now + ",v1=zz", "s")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verify(payload, WebhookVerifier.signHeader(payload, now, "other"), "s")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verifier.verify(payload, WebhookVerifier.signHeader(payload, now - 301, "s"), "s")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new WebhookVerifier(Duration.ofSeconds(-1), Clock.systemUTC())).isInstanceOf(IllegalArgumentException.class);
    }
}
