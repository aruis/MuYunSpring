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
        WorkflowNoticeReadStatus readStatus,
        String noticeSourceType,
        Integer delegationTaskCount,
        Boolean addedByAddSign,
        String addSignSourceNodeKey,
        String addSignOperatorId,
        Instant addSignAt
) {
    public WorkflowWorkbenchCard {
        currentAssigneeIds = currentAssigneeIds == null ? List.of() : List.copyOf(currentAssigneeIds);
        addedByAddSign = Boolean.TRUE.equals(addedByAddSign);
    }

    public WorkflowWorkbenchCard(String boardType,
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
                                 Integer delegationTaskCount) {
        this(boardType, instanceId, moduleAlias, recordId, definitionId, workflowVersionId, instanceStatus,
                approvalStatus, taskId, taskKind, taskStatus, nodeKey, currentNodeKeys, currentAssigneeIds,
                startedAt, receivedAt, completedAt, actionCode, overtimeStatus, dueAt, lastOperatedAt,
                assignmentKind, originalAssigneeId, delegatedFromUserId, delegatedToUserId, principalCanProcess,
                null, noticeSourceType, delegationTaskCount, false, null, null, null);
    }
}
