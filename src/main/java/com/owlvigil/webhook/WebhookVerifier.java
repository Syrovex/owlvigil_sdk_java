package com.owlvigil.webhook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class WebhookVerifier {
    private final Duration tolerance;
    private final Clock clock;

    public WebhookVerifier() {
        this(Duration.ofMinutes(5), Clock.systemUTC());
    }

    public WebhookVerifier(Duration tolerance, Clock clock) {
        if (tolerance == null || tolerance.isNegative()) throw new IllegalArgumentException("tolerance must not be negative");
        this.tolerance = tolerance;
        this.clock = clock;
    }

    public void verify(byte[] payload, String signatureHeader, String secret) {
        if (signatureHeader == null || signatureHeader.isEmpty()) throw new IllegalArgumentException("signature is required");
        if (secret == null || secret.trim().isEmpty()) throw new IllegalArgumentException("secret is required");
        long timestamp = 0;
        String signature = "";
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) continue;
            if (pair[0].equals("t")) {
                try { timestamp = Long.parseLong(pair[1]); }
                catch (NumberFormatException error) { throw new IllegalArgumentException("invalid webhook signature", error); }
            } else if (pair[0].equals("v1")) {
                signature = pair[1];
            }
        }
        if (timestamp == 0 || signature.isEmpty()) throw new IllegalArgumentException("invalid webhook signature");
        Duration age = Duration.between(Instant.ofEpochSecond(timestamp), clock.instant()).abs();
        if (age.compareTo(tolerance) > 0) throw new IllegalArgumentException("stale webhook timestamp");
        byte[] expected = sign(payload, timestamp, secret);
        byte[] actual = decodeHex(signature);
        if (!MessageDigest.isEqual(expected, actual)) throw new IllegalArgumentException("invalid webhook signature");
    }

    public static String signHeader(byte[] payload, long timestamp, String secret) {
        return "t=" + timestamp + ",v1=" + encodeHex(sign(payload, timestamp, secret));
    }

    private static byte[] sign(byte[] payload, long timestamp, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update(Long.toString(timestamp).getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            return mac.doFinal(payload);
        } catch (Exception error) {
            throw new IllegalStateException("HmacSHA256 is unavailable", error);
        }
    }

    private static byte[] decodeHex(String value) {
        if ((value.length() & 1) != 0) throw new IllegalArgumentException("invalid webhook signature");
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            int high = Character.digit(value.charAt(i * 2), 16);
            int low = Character.digit(value.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) throw new IllegalArgumentException("invalid webhook signature");
            result[i] = (byte) ((high << 4) | low);
        }
        return result;
    }

    private static String encodeHex(byte[] value) {
        char[] hex = "0123456789abcdef".toCharArray();
        char[] result = new char[value.length * 2];
        for (int i = 0; i < value.length; i++) {
            result[i * 2] = hex[(value[i] >>> 4) & 0x0f];
            result[i * 2 + 1] = hex[value[i] & 0x0f];
        }
        return new String(result);
    }
}
