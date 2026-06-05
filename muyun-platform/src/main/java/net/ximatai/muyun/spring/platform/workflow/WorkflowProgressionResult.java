package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowProgressionResult(
        WorkflowInstance instance,
        List<WorkflowNodeInstance> activatedNodes,
        List<WorkflowRouteInstance> selectedRoutes,
        List<WorkflowRouteInstance> droppedRoutes,
        List<WorkflowTask> createdTasks,
        List<WorkflowEvent> events,
        WorkflowActivationResult activation
) {
    public static WorkflowProgressionResult empty(WorkflowInstance instance) {
        return new WorkflowProgressionResult(instance, List.of(), List.of(), List.of(), List.of(), List.of(),
                new WorkflowActivationResult(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false));
    }
}
