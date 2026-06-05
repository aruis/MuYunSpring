package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowApprovalSummary(
        String moduleAlias,
        String recordId,
        String approvalInstanceId,
        WorkflowApprovalStatus approvalStatus,
        String approvalSubmittedBy,
        Instant approvalSubmittedAt,
        Instant approvalCompletedAt
) {
}
