package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowInstanceSnapshot(
        WorkflowInstance instance,
        List<WorkflowNodeInstance> nodes,
        List<WorkflowRouteInstance> routes,
        List<WorkflowEvent> events
) {
    public WorkflowInstanceSnapshot {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        routes = routes == null ? List.of() : List.copyOf(routes);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
