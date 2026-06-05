package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowRuntimeRenderBundle(
        String mode,
        WorkflowInstance instance,
        List<WorkflowNodeInstance> nodes,
        List<WorkflowRouteInstance> routes
) {
    public WorkflowRuntimeRenderBundle {
        mode = mode == null || mode.isBlank() ? "RUNTIME" : mode;
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        routes = routes == null ? List.of() : List.copyOf(routes);
    }
}
