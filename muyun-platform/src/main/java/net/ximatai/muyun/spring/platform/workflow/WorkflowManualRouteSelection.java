package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowManualRouteSelection(
        String branchNodeKey,
        String routeKey,
        String selectedReason
) {
}
