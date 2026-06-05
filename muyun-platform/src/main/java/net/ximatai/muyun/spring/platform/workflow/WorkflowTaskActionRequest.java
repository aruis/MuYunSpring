package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowTaskActionRequest(
        String taskId,
        String operatorId,
        String targetAssigneeId,
        WorkflowAddSignMode addSignMode,
        WorkflowAddSignSegment addSignSegment,
        WorkflowRejectResubmitMode rejectResubmitMode,
        String reason,
        Instant operatedAt
) {
    public WorkflowTaskActionRequest(String taskId, String operatorId, String targetAssigneeId,
                                     String reason, Instant operatedAt) {
        this(taskId, operatorId, targetAssigneeId, null, null, null, reason, operatedAt);
    }

    public WorkflowTaskActionRequest(String taskId, String operatorId, String targetAssigneeId,
                                     WorkflowRejectResubmitMode rejectResubmitMode,
                                     String reason, Instant operatedAt) {
        this(taskId, operatorId, targetAssigneeId, null, null, rejectResubmitMode, reason, operatedAt);
    }

    public WorkflowTaskActionRequest(String taskId, String operatorId, String targetAssigneeId,
                                     WorkflowAddSignMode addSignMode,
                                     WorkflowRejectResubmitMode rejectResubmitMode,
                                     String reason, Instant operatedAt) {
        this(taskId, operatorId, targetAssigneeId, addSignMode, null, rejectResubmitMode, reason, operatedAt);
    }

    public static WorkflowTaskActionRequest complete(String taskId, String operatorId, String reason) {
        return new WorkflowTaskActionRequest(taskId, operatorId, null, null, null, reason, null);
    }

    public static WorkflowTaskActionRequest reject(String taskId, String operatorId,
                                                   WorkflowRejectResubmitMode rejectResubmitMode,
                                                   String reason) {
        return new WorkflowTaskActionRequest(taskId, operatorId, null, null, rejectResubmitMode, reason, null);
    }

    public static WorkflowTaskActionRequest transfer(String taskId, String operatorId,
                                                     String targetAssigneeId, String reason) {
        return new WorkflowTaskActionRequest(taskId, operatorId, targetAssigneeId, null, null, reason, null);
    }

    public static WorkflowTaskActionRequest addSign(String taskId, String operatorId,
                                                    WorkflowAddSignSegment addSignSegment,
                                                    String reason) {
        return new WorkflowTaskActionRequest(taskId, operatorId, null, null, addSignSegment, null, reason, null);
    }
}
