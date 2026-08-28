package com.owlvigil.webhook;

import static org.assertj.core.api.Assertions.assertThatCode;

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
}
