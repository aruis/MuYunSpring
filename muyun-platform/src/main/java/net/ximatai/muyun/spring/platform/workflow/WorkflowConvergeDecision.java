package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowConvergeDecision(
        boolean passed,
        List<WorkflowRouteInstance> routesToClose,
        List<WorkflowRouteInstance> routesToDrop
) {
    public WorkflowConvergeDecision {
        routesToClose = routesToClose == null ? List.of() : List.copyOf(routesToClose);
        routesToDrop = routesToDrop == null ? List.of() : List.copyOf(routesToDrop);
    }

    public static WorkflowConvergeDecision waiting() {
        return new WorkflowConvergeDecision(false, List.of(), List.of());
    }
}
