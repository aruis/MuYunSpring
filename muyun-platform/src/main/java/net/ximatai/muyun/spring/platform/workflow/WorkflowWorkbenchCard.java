package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;
import java.util.List;

public record WorkflowWorkbenchCard(
        String boardType,
        String instanceId,
        String moduleAlias,
        String recordId,
        WorkflowInstanceStatus instanceStatus,
        WorkflowApprovalStatus approvalStatus,
        String taskId,
        WorkflowTaskKind taskKind,
        WorkflowTaskStatus taskStatus,
        String nodeKey,
        String currentNodeKeys,
        List<String> currentAssigneeIds,
        Instant receivedAt,
        Instant completedAt,
        String actionCode,
        WorkflowOvertimeStatus overtimeStatus,
        Instant lastOperatedAt,
        WorkflowAssignmentKind assignmentKind,
        String originalAssigneeId,
        String delegatedFromUserId,
        String delegatedToUserId,
        Boolean principalCanProcess
) {
    public WorkflowWorkbenchCard {
        currentAssigneeIds = currentAssigneeIds == null ? List.of() : List.copyOf(currentAssigneeIds);
    }
}
