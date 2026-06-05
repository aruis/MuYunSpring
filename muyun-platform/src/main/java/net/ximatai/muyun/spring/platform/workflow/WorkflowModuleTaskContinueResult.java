package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowModuleTaskContinueResult(
        boolean continued,
        WorkflowTaskActionResult actionResult,
        WorkflowModuleTaskProcessBundle processBundle
) {
    public static WorkflowModuleTaskContinueResult continued(WorkflowTaskActionResult actionResult) {
        return new WorkflowModuleTaskContinueResult(true, actionResult, null);
    }

    public static WorkflowModuleTaskContinueResult blocked(WorkflowModuleTaskProcessBundle processBundle) {
        return new WorkflowModuleTaskContinueResult(false, null, processBundle);
    }
}
