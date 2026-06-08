package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;
import java.util.List;

public record WorkflowWorkbenchQueryRequest(
        String moduleAlias,
        String recordId,
        String definitionId,
        String workflowVersionId,
        String definitionVersionId,
        WorkflowInstanceStatus instanceStatus,
        String nodeKey,
        WorkflowTaskKind taskKind,
        WorkflowTaskStatus taskStatus,
        WorkflowAssignmentKind assignmentKind,
        WorkflowOvertimeStatus overtimeStatus,
        WorkflowNoticeReadStatus readStatus,
        Instant startedFrom,
        Instant startedTo,
        Instant receivedFrom,
        Instant receivedTo,
        Instant completedFrom,
        Instant completedTo,
        Instant lastOperatedFrom,
        Instant lastOperatedTo,
        Instant dueFrom,
        Instant dueTo,
        Boolean addedByAddSign,
        String addSignSourceNodeKey,
        List<WorkflowWorkbenchSort> sorts
) {
    public WorkflowWorkbenchQueryRequest {
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    public static WorkflowWorkbenchQueryRequest empty() {
        return new WorkflowWorkbenchQueryRequest(null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    public WorkflowWorkbenchQueryRequest(String moduleAlias,
                                         String recordId,
                                         String definitionId,
                                         String workflowVersionId,
                                         String definitionVersionId,
                                         WorkflowInstanceStatus instanceStatus,
                                         String nodeKey,
                                         WorkflowTaskKind taskKind,
                                         WorkflowTaskStatus taskStatus,
                                         WorkflowAssignmentKind assignmentKind,
                                         WorkflowOvertimeStatus overtimeStatus,
                                         Instant startedFrom,
                                         Instant startedTo,
                                         Instant receivedFrom,
                                         Instant receivedTo,
                                         Instant completedFrom,
                                         Instant completedTo,
                                         Instant lastOperatedFrom,
                                         Instant lastOperatedTo,
                                         Instant dueFrom,
                                         Instant dueTo,
                                         List<WorkflowWorkbenchSort> sorts) {
        this(moduleAlias, recordId, definitionId, workflowVersionId, definitionVersionId, instanceStatus, nodeKey,
                taskKind, taskStatus, assignmentKind, overtimeStatus, null, startedFrom, startedTo, receivedFrom,
                receivedTo, completedFrom, completedTo, lastOperatedFrom, lastOperatedTo, dueFrom, dueTo,
                null, null, sorts);
    }

    public String effectiveWorkflowVersionId() {
        if (workflowVersionId != null && !workflowVersionId.isBlank()) {
            return workflowVersionId;
        }
        return definitionVersionId;
    }
}
