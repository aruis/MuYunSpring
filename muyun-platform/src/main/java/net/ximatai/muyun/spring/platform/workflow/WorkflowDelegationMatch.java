package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowDelegationMatch(
        String delegationPolicyId,
        String principalUserId,
        String delegateUserId,
        boolean principalCanProcess,
        String snapshotText
) {
}
