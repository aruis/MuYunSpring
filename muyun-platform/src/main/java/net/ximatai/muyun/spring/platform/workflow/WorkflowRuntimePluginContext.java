package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowRuntimePluginContext(
        WorkflowRuntimePluginEventType eventType,
        String actionCode,
        String moduleAlias,
        String recordId,
        String instanceId,
        String nodeKey,
        String taskId,
        String operatorId,
        String targetAssigneeId,
        String rollbackTargetNodeKey,
        WorkflowRuntimeTerminateMode terminateMode,
        String reason,
        WorkflowInstance instance,
        WorkflowNodeInstance node,
        WorkflowTask task
) {
    public WorkflowRuntimePluginContext {
        if (eventType == null) {
            throw new IllegalArgumentException("workflow runtime plugin event type must not be null");
        }
    }
}
