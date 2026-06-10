package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record DynamicRecordMutationEvent(
        String eventId,
        DynamicRecordMutationEventType eventType,
        String moduleAlias,
        String entityAlias,
        String recordId,
        DynamicRecordSaveOperation saveOperation,
        DynamicRecord beforeRecord,
        DynamicRecord afterRecord,
        RuntimeMutationSource mutationSource,
        String traceId,
        int depth,
        String parentExecutionId,
        boolean cascadeAllowed,
        Map<String, Object> metadata
) {
    public DynamicRecordMutationEvent {
        Objects.requireNonNull(eventType, "eventType must not be null");
        moduleAlias = requireText(moduleAlias, "moduleAlias");
        entityAlias = requireText(entityAlias, "entityAlias");
        recordId = requireText(recordId, "recordId");
        mutationSource = mutationSource == null ? RuntimeMutationSource.BUSINESS : mutationSource;
        eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        traceId = traceId == null || traceId.isBlank() ? eventId : traceId;
        if (depth < 0) {
            throw new PlatformException("dynamic mutation event depth must not be negative");
        }
        parentExecutionId = parentExecutionId == null || parentExecutionId.isBlank() ? null : parentExecutionId;
        metadata = metadata == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(metadata));
        validateSnapshotContract(eventType, saveOperation, beforeRecord, afterRecord);
    }

    public boolean shouldSkipForSingleHopCascade() {
        return mutationSource == RuntimeMutationSource.WRITE_BACK && !cascadeAllowed;
    }

    private static void validateSnapshotContract(DynamicRecordMutationEventType eventType,
                                                 DynamicRecordSaveOperation saveOperation,
                                                 DynamicRecord beforeRecord,
                                                 DynamicRecord afterRecord) {
        if (eventType == DynamicRecordMutationEventType.AFTER_SAVE) {
            if (saveOperation == null) {
                throw new PlatformException("AFTER_SAVE dynamic mutation event requires saveOperation");
            }
            if (afterRecord == null) {
                throw new PlatformException("AFTER_SAVE dynamic mutation event requires afterRecord");
            }
            if (saveOperation == DynamicRecordSaveOperation.UPDATE && beforeRecord == null) {
                throw new PlatformException("AFTER_SAVE update mutation event requires beforeRecord");
            }
            return;
        }
        if (eventType == DynamicRecordMutationEventType.AFTER_DELETE) {
            if (beforeRecord == null) {
                throw new PlatformException("AFTER_DELETE dynamic mutation event requires beforeRecord");
            }
            if (afterRecord != null) {
                throw new PlatformException("AFTER_DELETE dynamic mutation event afterRecord must be null");
            }
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PlatformException("dynamic mutation event " + fieldName + " must not be blank");
        }
        return value.trim();
    }
}
