package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowHistoryTaskView(
        String id,
        String instanceId,
        String nodeInstanceId,
        WorkflowTaskKind taskKind,
        WorkflowTaskStatus taskStatus,
        WorkflowAssignmentKind assignmentKind,
        String assigneeId,
        String actualProcessUserId,
        Boolean processedByDelegation,
        String originalAssigneeId,
        String delegatedFromUserId,
        String delegatedToUserId,
        Boolean principalCanProcess,
        String delegationPolicyId,
        String delegationSnapshot,
        Boolean invalidated,
        Boolean canceled,
        String decision,
        Instant receivedAt,
        Instant completedAt
) {
    public static WorkflowHistoryTaskView from(WorkflowTask task) {
        return new WorkflowHistoryTaskView(task.getId(), task.getInstanceId(), task.getNodeInstanceId(),
                task.getTaskKind(), task.getTaskStatus(), task.getAssignmentKind(), task.getAssigneeId(),
                task.getActualProcessorId(), WorkflowHistoryTaskViews.processedByDelegation(task),
                task.getOriginalAssigneeId(), task.getDelegatedFromUserId(), task.getDelegatedToUserId(),
                task.getPrincipalCanProcess(), task.getDelegationPolicyId(), task.getAssignmentSnapshotText(),
                task.getTaskStatus() == WorkflowTaskStatus.INVALIDATED,
                task.getTaskStatus() == WorkflowTaskStatus.CANCELED,
                task.getDecision(), task.getCreatedAt(), task.getCompletedAt());
    }
}
