package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowHistoryEventView(
        String id,
        String instanceId,
        String nodeInstanceId,
        String taskId,
        WorkflowEventType eventType,
        String actionCode,
        String operatorId,
        String actualProcessUserId,
        Boolean processedByDelegation,
        WorkflowAssignmentKind assignmentKind,
        String originalAssigneeId,
        String delegatedFromUserId,
        String delegatedToUserId,
        Boolean principalCanProcess,
        String delegationPolicyId,
        String delegationSnapshot,
        Boolean taskInvalidated,
        Boolean taskCanceled,
        String message,
        String payloadText,
        Instant occurredAt
) {
    public static WorkflowHistoryEventView from(WorkflowEvent event, WorkflowTask task) {
        return new WorkflowHistoryEventView(event.getId(), event.getInstanceId(), event.getNodeInstanceId(),
                event.getTaskId(), event.getEventType(), event.getActionCode(), event.getOperatorId(),
                task == null || task.getActualProcessorId() == null ? event.getOperatorId() : task.getActualProcessorId(),
                task == null ? Boolean.FALSE : WorkflowHistoryTaskViews.processedByDelegation(task),
                task == null ? null : task.getAssignmentKind(),
                task == null ? null : task.getOriginalAssigneeId(),
                task == null ? null : task.getDelegatedFromUserId(),
                task == null ? null : task.getDelegatedToUserId(),
                task == null ? null : task.getPrincipalCanProcess(),
                task == null ? null : task.getDelegationPolicyId(),
                task == null ? null : task.getAssignmentSnapshotText(),
                task != null && task.getTaskStatus() == WorkflowTaskStatus.INVALIDATED,
                task != null && task.getTaskStatus() == WorkflowTaskStatus.CANCELED,
                event.getMessage(), event.getPayloadText(), event.getOccurredAt());
    }
}
