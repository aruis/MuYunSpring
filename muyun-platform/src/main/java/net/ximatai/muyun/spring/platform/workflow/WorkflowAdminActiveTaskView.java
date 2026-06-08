package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowAdminActiveTaskView(
        String taskId,
        String instanceId,
        String nodeInstanceId,
        String nodeKey,
        WorkflowTaskKind taskKind,
        WorkflowTaskStatus taskStatus,
        String assigneeId,
        Instant createdAt,
        Instant receivedAt,
        WorkflowOvertimeStatus overtimeStatus,
        boolean canForceApprove,
        WorkflowAssignmentKind assignmentKind,
        String originalAssigneeId,
        String delegatedFromUserId,
        String delegatedToUserId,
        Boolean principalCanProcess,
        String delegationPolicyId,
        String delegationSnapshot,
        Boolean addedByAddSign,
        String addSignSourceNodeKey,
        String addSignOperatorId,
        Instant addSignAt
) {
    public WorkflowAdminActiveTaskView {
        boolean isAddedByAddSign = Boolean.TRUE.equals(addedByAddSign);
        addedByAddSign = isAddedByAddSign;
        if (!isAddedByAddSign) {
            addSignSourceNodeKey = null;
            addSignOperatorId = null;
            addSignAt = null;
        }
    }

    public static WorkflowAdminActiveTaskView from(WorkflowTask task, WorkflowNodeInstance node) {
        boolean addedByAddSign = Boolean.TRUE.equals(node.getAddedByAddSign());
        return new WorkflowAdminActiveTaskView(
                task.getId(),
                task.getInstanceId(),
                task.getNodeInstanceId(),
                node.getNodeKey(),
                task.getTaskKind(),
                task.getTaskStatus(),
                task.getAssigneeId(),
                task.getCreatedAt(),
                task.getCreatedAt(),
                node.getOvertimeStatus(),
                true,
                task.getAssignmentKind(),
                task.getOriginalAssigneeId(),
                task.getDelegatedFromUserId(),
                task.getDelegatedToUserId(),
                task.getPrincipalCanProcess(),
                task.getDelegationPolicyId(),
                task.getAssignmentSnapshotText(),
                addedByAddSign,
                addedByAddSign ? blankToNull(node.getAddSignSourceNodeKey()) : null,
                addedByAddSign ? blankToNull(node.getAddSignOperatorId()) : null,
                addedByAddSign ? node.getAddSignAt() : null);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
