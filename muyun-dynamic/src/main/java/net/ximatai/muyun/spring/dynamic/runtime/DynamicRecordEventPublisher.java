package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.ability.event.ActionEventPayload;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;

import java.util.List;
import java.util.Map;

final class DynamicRecordEventPublisher {
    private final RuntimeEventPublisher publisher;

    DynamicRecordEventPublisher(RuntimeEventPublisher publisher) {
        this.publisher = publisher == null ? RuntimeEventPublisher.noop() : publisher;
    }

    void created(DynamicRecordEventContext context, String recordId) {
        recordChanged(RuntimeEventType.AFTER_CREATE, context, recordId, Map.of());
    }

    void updated(DynamicRecordEventContext context, String recordId) {
        recordChanged(RuntimeEventType.AFTER_UPDATE, context, recordId, Map.of());
    }

    void deleted(DynamicRecordEventContext context, String recordId) {
        recordChanged(RuntimeEventType.AFTER_DELETE, context, recordId, Map.of());
    }

    void deletedBatch(DynamicRecordEventContext context, List<String> recordIds, int count) {
        recordChanged(RuntimeEventType.AFTER_DELETE, context, null,
                Map.of("recordIds", List.copyOf(recordIds), "count", count));
    }

    void reordered(DynamicRecordEventContext context, List<String> orderedIds) {
        recordChanged(RuntimeEventType.AFTER_UPDATE, context, null,
                Map.of("recordIds", List.copyOf(orderedIds), "operation", "reorder"));
    }

    void movedBefore(DynamicRecordEventContext context, String recordId, String beforeId) {
        recordChanged(RuntimeEventType.AFTER_UPDATE, context, recordId,
                Map.of("beforeId", beforeId, "operation", "moveBefore"));
    }

    void movedAfter(DynamicRecordEventContext context, String recordId, String afterId) {
        recordChanged(RuntimeEventType.AFTER_UPDATE, context, recordId,
                Map.of("afterId", afterId, "operation", "moveAfter"));
    }

    void movedInTree(DynamicRecordEventContext context,
                     String recordId,
                     String previousId,
                     String nextId,
                     String parentId) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("operation", "moveInTree");
        putIfPresent(payload, "previousId", previousId);
        putIfPresent(payload, "nextId", nextId);
        putIfPresent(payload, "parentId", parentId);
        recordChanged(RuntimeEventType.AFTER_UPDATE, context, recordId, payload);
    }

    void enabled(DynamicRecordEventContext context, String recordId) {
        recordChanged(RuntimeEventType.AFTER_UPDATE, context, recordId, Map.of("operation", "enable"));
    }

    void disabled(DynamicRecordEventContext context, String recordId) {
        recordChanged(RuntimeEventType.AFTER_UPDATE, context, recordId, Map.of("operation", "disable"));
    }

    void actionExecuted(DynamicActionExecutionContext context, DynamicActionResultBody body) {
        publisher.publishAfterCommit(new RuntimeEvent(
                null,
                context.traceId(),
                RuntimeEventType.ACTION_EXECUTED,
                context.moduleAlias(),
                context.entityAlias(),
                context.recordId(),
                context.actionCode(),
                context.tenantId(),
                context.systemContext(),
                context.systemReason(),
                context.operatorId(),
                context.operatorType(),
                context.authorizationDecision(),
                context.authorizationPermissionCode(),
                context.authorizationPermissionActionCode(),
                RuntimeMutationSource.ACTION,
                actionPayload(context, ActionEventPayload.executed(
                        context.action().executorType().name(),
                        context.action().actionLevel().name(),
                        body.type().name(),
                        body.message(),
                        body.refresh(),
                        body.redirectTo(),
                        context.action().executorType() == EntityActionExecutorType.DIALOG,
                        isSimpleEventValue(body.value()) ? body.value() : null
                )),
                null
        ));
    }

    void actionFailed(DynamicActionExecutionContext context,
                      String failureStage,
                      String errorMessage,
                      Throwable cause) {
        try {
            publisher.publish(new RuntimeEvent(
                    null,
                    context.traceId(),
                    RuntimeEventType.ACTION_FAILED,
                    context.moduleAlias(),
                    context.entityAlias(),
                    context.recordId(),
                    context.actionCode(),
                    context.tenantId(),
                    context.systemContext(),
                    context.systemReason(),
                    context.operatorId(),
                    context.operatorType(),
                    context.authorizationDecision(),
                    context.authorizationPermissionCode(),
                    context.authorizationPermissionActionCode(),
                    RuntimeMutationSource.ACTION,
                    actionPayload(context, ActionEventPayload.failed(
                            context.action().executorType().name(),
                            context.action().actionLevel().name(),
                            context.availability().available(),
                            failureStage,
                            errorMessage,
                            cause == null ? null : cause.getClass().getName()
                    )),
                    null
            ));
        } catch (RuntimeException ignored) {
            // Failure audit must not replace the original action failure.
        }
    }

    private void recordChanged(RuntimeEventType eventType,
                               DynamicRecordEventContext context,
                               String recordId,
                               Map<String, Object> payload) {
        publisher.publishAfterCommit(RuntimeEvent.of(
                context.traceId(),
                eventType,
                context.moduleAlias(),
                context.entityAlias(),
                recordId,
                null,
                context.tenantId(),
                context.systemContext(),
                context.systemReason(),
                context.mutationSource(),
                recordPayload(context, payload)
        ));
    }

    private Map<String, Object> actionPayload(DynamicActionExecutionContext context, Map<String, Object> payload) {
        return context.systemContext()
                ? ActionEventPayload.withSystemReason(payload, context.systemReason())
                : payload;
    }

    private Map<String, Object> recordPayload(DynamicRecordEventContext context, Map<String, Object> payload) {
        return context.systemContext()
                ? ActionEventPayload.withSystemReason(payload, context.systemReason())
                : payload;
    }

    private void putIfPresent(Map<String, Object> payload, String key, String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }

    private boolean isSimpleEventValue(Object value) {
        return value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Enum<?>;
    }

    record DynamicRecordEventContext(
            String moduleAlias,
            String entityAlias,
            String traceId,
            String tenantId,
            boolean systemContext,
            String systemReason,
            RuntimeMutationSource mutationSource
    ) {
    }
}
