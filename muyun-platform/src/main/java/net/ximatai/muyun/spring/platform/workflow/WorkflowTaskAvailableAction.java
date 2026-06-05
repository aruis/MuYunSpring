package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowTaskAvailableAction(
        String actionCode,
        String title,
        boolean reasonRequired,
        boolean targetAssigneeRequired,
        boolean rejectReturnToMeSupported,
        String taskId,
        String nodeKey,
        String nodeTitle,
        List<String> rejectResubmitModes,
        String defaultRejectResubmitMode,
        WorkflowAssignmentKind assignmentKind,
        String originalAssigneeId,
        String delegatedFromUserId,
        String delegatedToUserId,
        Boolean principalCanProcess
) {
    public WorkflowTaskAvailableAction(String actionCode,
                                       String title,
                                       boolean reasonRequired,
                                       boolean targetAssigneeRequired,
                                       boolean rejectReturnToMeSupported) {
        this(actionCode, title, reasonRequired, targetAssigneeRequired, rejectReturnToMeSupported,
                null, null, null, List.of(), null, null, null, null, null, null);
    }

    public WorkflowTaskAvailableAction {
        rejectResubmitModes = rejectResubmitModes == null ? List.of() : List.copyOf(rejectResubmitModes);
    }

    public static WorkflowTaskAvailableAction of(String actionCode, String title) {
        return new WorkflowTaskAvailableAction(actionCode, title, false, false, false);
    }

    public WorkflowTaskAvailableAction requireReason(boolean required) {
        return new WorkflowTaskAvailableAction(actionCode, title, required, targetAssigneeRequired,
                rejectReturnToMeSupported, taskId, nodeKey, nodeTitle, rejectResubmitModes, defaultRejectResubmitMode,
                assignmentKind, originalAssigneeId, delegatedFromUserId, delegatedToUserId, principalCanProcess);
    }

    public WorkflowTaskAvailableAction requireTargetAssignee() {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, true, rejectReturnToMeSupported,
                taskId, nodeKey, nodeTitle, rejectResubmitModes, defaultRejectResubmitMode, assignmentKind,
                originalAssigneeId, delegatedFromUserId, delegatedToUserId, principalCanProcess);
    }

    public WorkflowTaskAvailableAction supportRejectReturnToMe(boolean supported) {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, targetAssigneeRequired, supported,
                taskId, nodeKey, nodeTitle, rejectResubmitModes, defaultRejectResubmitMode, assignmentKind,
                originalAssigneeId, delegatedFromUserId, delegatedToUserId, principalCanProcess);
    }

    public WorkflowTaskAvailableAction withTask(WorkflowTask task, WorkflowNodeInstance node) {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, targetAssigneeRequired,
                rejectReturnToMeSupported, task == null ? null : task.getId(), node == null ? null : node.getNodeKey(),
                node == null ? null : node.getNodeKey(), rejectResubmitModes, defaultRejectResubmitMode,
                task == null ? null : task.getAssignmentKind(), task == null ? null : task.getOriginalAssigneeId(),
                task == null ? null : task.getDelegatedFromUserId(),
                task == null ? null : task.getAssigneeId(), null);
    }

    public WorkflowTaskAvailableAction withRejectResubmitModes(List<String> modes, String defaultMode) {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, targetAssigneeRequired,
                rejectReturnToMeSupported, taskId, nodeKey, nodeTitle, modes, defaultMode, assignmentKind,
                originalAssigneeId, delegatedFromUserId, delegatedToUserId, principalCanProcess);
    }
}
