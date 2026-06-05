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
        Instant operatedAt,
        String selectedRouteKey,
        String selectedReason
) {
    public WorkflowTaskActionRequest(String taskId, String operatorId, String targetAssigneeId,
                                     String reason, Instant operatedAt) {
        this(taskId, operatorId, targetAssigneeId, null, null, null, reason, operatedAt, null, null);
    }

    public WorkflowTaskActionRequest(String taskId, String operatorId, String targetAssigneeId,
                                     WorkflowRejectResubmitMode rejectResubmitMode,
                                     String reason, Instant operatedAt) {
        this(taskId, operatorId, targetAssigneeId, null, null, rejectResubmitMode, reason, operatedAt, null, null);
    }

    public WorkflowTaskActionRequest(String taskId, String operatorId, String targetAssigneeId,
                                     WorkflowAddSignMode addSignMode,
                                     WorkflowRejectResubmitMode rejectResubmitMode,
                                     String reason, Instant operatedAt) {
        this(taskId, operatorId, targetAssigneeId, addSignMode, null, rejectResubmitMode, reason, operatedAt,
                null, null);
    }

    public WorkflowTaskActionRequest(String taskId, String operatorId, String targetAssigneeId,
                                     WorkflowAddSignMode addSignMode,
                                     WorkflowAddSignSegment addSignSegment,
                                     WorkflowRejectResubmitMode rejectResubmitMode,
                                     String reason, Instant operatedAt) {
        this(taskId, operatorId, targetAssigneeId, addSignMode, addSignSegment, rejectResubmitMode, reason, operatedAt,
                null, null);
    }

    public static WorkflowTaskActionRequest complete(String taskId, String operatorId, String reason) {
        return new WorkflowTaskActionRequest(taskId, operatorId, null, null, null, reason, null);
    }

    public static WorkflowTaskActionRequest complete(String taskId, String operatorId, String reason,
                                                     String selectedRouteKey, String selectedReason) {
        return new WorkflowTaskActionRequest(taskId, operatorId, null, null, null, null, reason, null,
                selectedRouteKey, selectedReason);
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
