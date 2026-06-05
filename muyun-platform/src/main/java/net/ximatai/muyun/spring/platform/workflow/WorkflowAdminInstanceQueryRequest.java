package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowAdminInstanceQueryRequest(
        String moduleAlias,
        String recordId,
        String starterId,
        WorkflowInstanceStatus instanceStatus,
        WorkflowApprovalStatus approvalStatus,
        String currentAssigneeId,
        WorkflowOvertimeStatus overtimeStatus
) {
    public static WorkflowAdminInstanceQueryRequest empty() {
        return new WorkflowAdminInstanceQueryRequest(null, null, null, null, null, null, null);
    }
}
