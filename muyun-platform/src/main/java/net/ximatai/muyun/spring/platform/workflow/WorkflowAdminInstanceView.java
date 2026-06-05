package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;
import java.util.List;

public record WorkflowAdminInstanceView(
        String instanceId,
        String moduleAlias,
        String recordId,
        String definitionId,
        String workflowVersionId,
        Integer versionNo,
        WorkflowInstanceStatus instanceStatus,
        WorkflowApprovalStatus approvalStatus,
        String startedBy,
        Instant startedAt,
        List<String> activeNodeKeys,
        List<String> currentTaskIds,
        List<String> currentAssigneeIds,
        WorkflowOvertimeStatus overtimeStatus,
        Instant updatedAt,
        Instant lastOperatedAt
) {
    public WorkflowAdminInstanceView {
        activeNodeKeys = activeNodeKeys == null ? List.of() : List.copyOf(activeNodeKeys);
        currentTaskIds = currentTaskIds == null ? List.of() : List.copyOf(currentTaskIds);
        currentAssigneeIds = currentAssigneeIds == null ? List.of() : List.copyOf(currentAssigneeIds);
    }
}
