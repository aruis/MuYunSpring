package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowSubmitStatusView(
        String moduleAlias,
        String recordId,
        String displayStatus,
        String instanceId,
        WorkflowInstanceStatus instanceStatus,
        WorkflowApprovalStatus approvalStatus,
        WorkflowDefinitionSummaryView definition,
        String errorMessage
) {
    public static WorkflowSubmitStatusView current(WorkflowInstance instance) {
        return new WorkflowSubmitStatusView(
                instance.getModuleAlias(),
                instance.getRecordId(),
                displayStatus(instance),
                instance.getId(),
                instance.getInstanceStatus(),
                instance.getApprovalStatus(),
                WorkflowDefinitionSummaryView.of(instance),
                null);
    }

    public static WorkflowSubmitStatusView unsubmitted(String moduleAlias, String recordId,
                                                       WorkflowDefinitionSelection selection) {
        return new WorkflowSubmitStatusView(moduleAlias, recordId, "UNSUBMITTED", null, null, null,
                WorkflowDefinitionSummaryView.of(selection), null);
    }

    public static WorkflowSubmitStatusView noWorkflow(String moduleAlias, String recordId, String errorMessage) {
        return new WorkflowSubmitStatusView(moduleAlias, recordId, "NO_WORKFLOW", null, null, null, null,
                errorMessage);
    }

    public static WorkflowSubmitStatusView matchError(String moduleAlias, String recordId, String errorMessage) {
        return new WorkflowSubmitStatusView(moduleAlias, recordId, "MATCH_ERROR", null, null, null, null,
                errorMessage);
    }

    private static String displayStatus(WorkflowInstance instance) {
        if (Boolean.TRUE.equals(instance.getApprovalEnabled()) && instance.getApprovalStatus() != null) {
            return instance.getApprovalStatus().name();
        }
        return instance.getInstanceStatus() == null ? "RUNNING" : instance.getInstanceStatus().name();
    }
}
