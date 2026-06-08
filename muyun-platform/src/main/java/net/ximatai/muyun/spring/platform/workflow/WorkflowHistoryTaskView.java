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
        String assigneeTitle,
        String actualProcessUserId,
        String actualProcessUserTitle,
        Boolean processedByDelegation,
        String originalAssigneeId,
        String originalAssigneeTitle,
        String delegatedFromUserId,
        String delegatedFromUserTitle,
        String delegatedToUserId,
        String delegatedToUserTitle,
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
        return from(task, java.util.Map.of());
    }

    public static WorkflowHistoryTaskView from(WorkflowTask task, java.util.Map<String, String> userTitles) {
        return new WorkflowHistoryTaskView(task.getId(), task.getInstanceId(), task.getNodeInstanceId(),
                task.getTaskKind(), task.getTaskStatus(), task.getAssignmentKind(), task.getAssigneeId(),
                title(task.getAssigneeId(), userTitles), task.getActualProcessorId(),
                title(task.getActualProcessorId(), userTitles), WorkflowHistoryTaskViews.processedByDelegation(task),
                task.getOriginalAssigneeId(), title(task.getOriginalAssigneeId(), userTitles),
                task.getDelegatedFromUserId(), title(task.getDelegatedFromUserId(), userTitles),
                task.getDelegatedToUserId(), title(task.getDelegatedToUserId(), userTitles),
                task.getPrincipalCanProcess(), task.getDelegationPolicyId(), task.getAssignmentSnapshotText(),
                task.getTaskStatus() == WorkflowTaskStatus.INVALIDATED,
                task.getTaskStatus() == WorkflowTaskStatus.CANCELED,
                task.getDecision(), task.getCreatedAt(), task.getCompletedAt());
    }

    private static String title(String userId, java.util.Map<String, String> userTitles) {
        return userId == null || userTitles == null ? null : userTitles.get(userId);
    }
}
