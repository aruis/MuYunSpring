package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowActivationResult(
        List<String> activatedNodeKeys,
        List<String> traversedRouteKeys,
        List<String> blockingApprovalNodeKeys,
        List<String> blockingTaskNodeKeys,
        List<String> waitingConvergeNodeKeys,
        List<WorkflowMilestoneType> reachedMilestones,
        boolean completed
) {
    public WorkflowActivationResult {
        activatedNodeKeys = activatedNodeKeys == null ? List.of() : List.copyOf(activatedNodeKeys);
        traversedRouteKeys = traversedRouteKeys == null ? List.of() : List.copyOf(traversedRouteKeys);
        blockingApprovalNodeKeys = blockingApprovalNodeKeys == null ? List.of() : List.copyOf(blockingApprovalNodeKeys);
        blockingTaskNodeKeys = blockingTaskNodeKeys == null ? List.of() : List.copyOf(blockingTaskNodeKeys);
        waitingConvergeNodeKeys = waitingConvergeNodeKeys == null ? List.of() : List.copyOf(waitingConvergeNodeKeys);
        reachedMilestones = reachedMilestones == null ? List.of() : List.copyOf(reachedMilestones);
    }

    public boolean approvalCompleted() {
        return reachedMilestones.contains(WorkflowMilestoneType.APPROVAL_COMPLETED);
    }
}
