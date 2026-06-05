package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowTaskActionRequest(
        String taskId,
        String operatorId,
        String targetAssigneeId,
        String reason,
        Instant operatedAt
) {
    public static WorkflowTaskActionRequest complete(String taskId, String operatorId, String reason) {
        return new WorkflowTaskActionRequest(taskId, operatorId, null, reason, null);
    }

    public static WorkflowTaskActionRequest transfer(String taskId, String operatorId,
                                                     String targetAssigneeId, String reason) {
        return new WorkflowTaskActionRequest(taskId, operatorId, targetAssigneeId, reason, null);
    }
}
