package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record WorkflowActivationRequest(
        WorkflowRuntimeGraph graph,
        List<WorkflowActivationTarget> targets,
        Map<String, Set<String>> selectedRouteKeysByBranch,
        Set<String> passedConvergeNodeKeys,
        int maxSteps
) {
    public WorkflowActivationRequest {
        targets = targets == null ? List.of() : List.copyOf(targets);
        selectedRouteKeysByBranch = selectedRouteKeysByBranch == null ? Map.of() : Map.copyOf(selectedRouteKeysByBranch);
        passedConvergeNodeKeys = passedConvergeNodeKeys == null ? Set.of() : Set.copyOf(passedConvergeNodeKeys);
        maxSteps = maxSteps <= 0 ? 512 : maxSteps;
    }

    public static WorkflowActivationRequest from(WorkflowRuntimeGraph graph, String targetNodeKey) {
        return new WorkflowActivationRequest(graph, List.of(WorkflowActivationTarget.of(targetNodeKey)),
                Map.of(), Set.of(), 512);
    }
}
