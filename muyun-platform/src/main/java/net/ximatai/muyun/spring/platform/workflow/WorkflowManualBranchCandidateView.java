package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowManualBranchCandidateView(
        String branchNodeKey,
        WorkflowRouteMode routeMode,
        String selectorNodeKey,
        Boolean requireManualSelectionReason,
        List<Candidate> candidates
) {
    public WorkflowManualBranchCandidateView {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public record Candidate(
            String routeId,
            String routeKey,
            String targetNodeKey,
            WorkflowNodeType targetNodeType,
            WorkflowRouteStatus routeStatus,
            Boolean defaultRoute
    ) {
    }
}
