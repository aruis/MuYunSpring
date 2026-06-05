package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowSubmitDraft(
        WorkflowInstance instance,
        List<WorkflowNodeInstance> nodes,
        List<WorkflowRouteInstance> routes,
        List<WorkflowTask> tasks,
        List<WorkflowEvent> events,
        WorkflowActivationResult activation
) {
    public WorkflowSubmitDraft {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        routes = routes == null ? List.of() : List.copyOf(routes);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
