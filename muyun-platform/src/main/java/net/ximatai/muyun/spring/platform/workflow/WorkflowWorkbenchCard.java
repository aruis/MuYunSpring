package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;
import java.util.List;

public record WorkflowWorkbenchCard(
        String boardType,
        String instanceId,
        String moduleAlias,
        String recordId,
        String definitionId,
        String workflowVersionId,
        WorkflowInstanceStatus instanceStatus,
        WorkflowApprovalStatus approvalStatus,
        String taskId,
        WorkflowTaskKind taskKind,
        WorkflowTaskStatus taskStatus,
        String nodeKey,
        String currentNodeKeys,
        List<String> currentAssigneeIds,
        Instant startedAt,
        Instant receivedAt,
        Instant completedAt,
        String actionCode,
        WorkflowOvertimeStatus overtimeStatus,
        Instant dueAt,
        Instant lastOperatedAt,
        WorkflowAssignmentKind assignmentKind,
        String originalAssigneeId,
        String delegatedFromUserId,
        String delegatedToUserId,
        Boolean principalCanProcess,
        String noticeSourceType,
        Integer delegationTaskCount
) {
    public WorkflowWorkbenchCard {
        currentAssigneeIds = currentAssigneeIds == null ? List.of() : List.copyOf(currentAssigneeIds);
    }
}
