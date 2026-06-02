package net.ximatai.muyun.spring.ability.event;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record RuntimeEvent(
        String eventId,
        String traceId,
        RuntimeEventType eventType,
        String moduleAlias,
        String entityAlias,
        String recordId,
        String actionCode,
        String tenantId,
        boolean systemContext,
        RuntimeMutationSource mutationSource,
        Map<String, Object> payload,
        Instant occurredAt
) {
    public RuntimeEvent {
        Objects.requireNonNull(eventType, "eventType must not be null");
        requireText(moduleAlias, "moduleAlias");
        if (eventType.requiresEntityAlias()) {
            requireText(entityAlias, "entityAlias");
        }
        Objects.requireNonNull(mutationSource, "mutationSource must not be null");
        if (eventType == RuntimeEventType.ACTION_EXECUTED || eventType == RuntimeEventType.ACTION_FAILED) {
            requireText(actionCode, "actionCode");
        }
        eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        traceId = traceId == null || traceId.isBlank() ? eventId : traceId;
        payload = payload == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(payload));
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }

    public static RuntimeEvent of(RuntimeEventType eventType,
                                  String moduleAlias,
                                  String entityAlias,
                                  String recordId,
                                  String actionCode,
                                  String tenantId,
                                  boolean systemContext,
                                  RuntimeMutationSource mutationSource,
                                  Map<String, Object> payload) {
        return new RuntimeEvent(null, null, eventType, moduleAlias, entityAlias, recordId, actionCode,
                tenantId, systemContext, mutationSource, payload, null);
    }

    public static RuntimeEvent of(String traceId,
                                  RuntimeEventType eventType,
                                  String moduleAlias,
                                  String entityAlias,
                                  String recordId,
                                  String actionCode,
                                  String tenantId,
                                  boolean systemContext,
                                  RuntimeMutationSource mutationSource,
                                  Map<String, Object> payload) {
        return new RuntimeEvent(null, traceId, eventType, moduleAlias, entityAlias, recordId, actionCode,
                tenantId, systemContext, mutationSource, payload, null);
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
