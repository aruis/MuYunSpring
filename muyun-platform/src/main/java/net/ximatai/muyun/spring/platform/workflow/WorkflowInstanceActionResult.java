package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowInstanceActionResult(
        WorkflowInstance instance,
        List<WorkflowTask> affectedTasks,
        List<WorkflowNodeInstance> affectedNodes,
        List<WorkflowRouteInstance> affectedRoutes,
        WorkflowEvent event
) {
}
