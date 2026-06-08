package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowManualBranchCandidatePrecheckView(
        String branchNodeKey,
        WorkflowRouteMode routeMode,
        String selectorNodeKey,
        Boolean requireManualSelectionReason,
        String selectorResolvedUserId,
        String operatorId,
        Boolean selectable,
        String unselectableReason,
        List<Candidate> candidates
) {
    public WorkflowManualBranchCandidatePrecheckView {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public record Candidate(
            String routeId,
            String routeKey,
            String targetNodeKey,
            WorkflowNodeType targetNodeType,
            WorkflowRouteStatus routeStatus,
            Boolean defaultRoute,
            Boolean selectable,
            String unselectableReason
    ) {
    }
}
