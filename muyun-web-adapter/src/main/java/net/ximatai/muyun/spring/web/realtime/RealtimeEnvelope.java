package net.ximatai.muyun.spring.web.realtime;

import net.ximatai.muyun.spring.common.id.Ids;

import java.time.Instant;

public record RealtimeEnvelope<T>(
        String id,
        String type,
        Instant occurredAt,
        String traceId,
        T payload
) {
    public RealtimeEnvelope {
        id = id == null || id.isBlank() ? Ids.newId() : id.trim();
        type = requireText(type, "type");
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static <T> RealtimeEnvelope<T> of(String type, String traceId, T payload) {
        return new RealtimeEnvelope<>(Ids.newId(), type, Instant.now(), traceId, payload);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        return value.trim();
    }
}
