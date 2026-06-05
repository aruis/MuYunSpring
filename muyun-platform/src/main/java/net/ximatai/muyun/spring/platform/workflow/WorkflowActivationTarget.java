package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;

public record WorkflowActivationTarget(String nodeKey, String routeInstanceId) {
    public WorkflowActivationTarget {
        if (nodeKey == null || nodeKey.isBlank()) {
            throw new PlatformException("workflow activation target node key must not be blank");
        }
    }

    public static WorkflowActivationTarget of(String nodeKey) {
        return new WorkflowActivationTarget(nodeKey, null);
    }
}
