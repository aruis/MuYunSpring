package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowInstanceActionRequest(
        String instanceId,
        String operatorId,
        String reason,
        Instant operatedAt
) {
    public static WorkflowInstanceActionRequest revoke(String instanceId, String operatorId, String reason) {
        return new WorkflowInstanceActionRequest(instanceId, operatorId, reason, null);
    }

    public static WorkflowInstanceActionRequest terminate(String instanceId, String operatorId, String reason) {
        return new WorkflowInstanceActionRequest(instanceId, operatorId, reason, null);
    }

    public static WorkflowInstanceActionRequest reset(String instanceId, String operatorId, String reason) {
        return new WorkflowInstanceActionRequest(instanceId, operatorId, reason, null);
    }
}
