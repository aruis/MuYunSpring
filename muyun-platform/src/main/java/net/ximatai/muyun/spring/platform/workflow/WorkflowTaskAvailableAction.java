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
        String originalAssigneeTitle,
        String delegatedFromUserId,
        String delegatedFromUserTitle,
        String delegatedToUserId,
        String delegatedToUserTitle,
        Boolean principalCanProcess
) {
    public WorkflowTaskAvailableAction(String actionCode,
                                       String title,
                                       boolean reasonRequired,
                                       boolean targetAssigneeRequired,
                                       boolean rejectReturnToMeSupported) {
        this(actionCode, title, reasonRequired, targetAssigneeRequired, rejectReturnToMeSupported,
                null, null, null, List.of(), null, null, null, null, null, null, null, null, null);
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
                assignmentKind, originalAssigneeId, originalAssigneeTitle, delegatedFromUserId,
                delegatedFromUserTitle, delegatedToUserId, delegatedToUserTitle, principalCanProcess);
    }

    public WorkflowTaskAvailableAction requireTargetAssignee() {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, true, rejectReturnToMeSupported,
                taskId, nodeKey, nodeTitle, rejectResubmitModes, defaultRejectResubmitMode, assignmentKind,
                originalAssigneeId, originalAssigneeTitle, delegatedFromUserId, delegatedFromUserTitle,
                delegatedToUserId, delegatedToUserTitle, principalCanProcess);
    }

    public WorkflowTaskAvailableAction supportRejectReturnToMe(boolean supported) {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, targetAssigneeRequired, supported,
                taskId, nodeKey, nodeTitle, rejectResubmitModes, defaultRejectResubmitMode, assignmentKind,
                originalAssigneeId, originalAssigneeTitle, delegatedFromUserId, delegatedFromUserTitle,
                delegatedToUserId, delegatedToUserTitle, principalCanProcess);
    }

    public WorkflowTaskAvailableAction withTask(WorkflowTask task, WorkflowNodeInstance node) {
        return withTask(task, node, java.util.Map.of());
    }

    public WorkflowTaskAvailableAction withTask(WorkflowTask task, WorkflowNodeInstance node,
                                                java.util.Map<String, String> userTitles) {
        String delegatedTo = task == null ? null : firstText(task.getDelegatedToUserId(), task.getAssigneeId());
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, targetAssigneeRequired,
                rejectReturnToMeSupported, task == null ? null : task.getId(), node == null ? null : node.getNodeKey(),
                node == null ? null : firstText(node.getNodeTitle(), node.getNodeKey()), rejectResubmitModes,
                defaultRejectResubmitMode,
                task == null ? null : task.getAssignmentKind(), task == null ? null : task.getOriginalAssigneeId(),
                title(task == null ? null : task.getOriginalAssigneeId(), userTitles),
                task == null ? null : task.getDelegatedFromUserId(),
                title(task == null ? null : task.getDelegatedFromUserId(), userTitles),
                delegatedTo,
                title(delegatedTo, userTitles),
                task == null ? null : task.getPrincipalCanProcess());
    }

    public WorkflowTaskAvailableAction withRejectResubmitModes(List<String> modes, String defaultMode) {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, targetAssigneeRequired,
                rejectReturnToMeSupported, taskId, nodeKey, nodeTitle, modes, defaultMode, assignmentKind,
                originalAssigneeId, originalAssigneeTitle, delegatedFromUserId, delegatedFromUserTitle,
                delegatedToUserId, delegatedToUserTitle, principalCanProcess);
    }

    private static String firstText(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private static String title(String userId, java.util.Map<String, String> userTitles) {
        return userId == null || userTitles == null ? null : userTitles.get(userId);
    }
}
