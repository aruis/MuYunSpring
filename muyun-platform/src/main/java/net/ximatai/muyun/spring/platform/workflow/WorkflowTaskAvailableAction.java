package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowTaskAvailableAction(
        String actionCode,
        String title,
        boolean reasonRequired,
        boolean targetAssigneeRequired,
        boolean rejectReturnToMeSupported
) {
    public static WorkflowTaskAvailableAction of(String actionCode, String title) {
        return new WorkflowTaskAvailableAction(actionCode, title, false, false, false);
    }

    public WorkflowTaskAvailableAction requireReason(boolean required) {
        return new WorkflowTaskAvailableAction(actionCode, title, required, targetAssigneeRequired,
                rejectReturnToMeSupported);
    }

    public WorkflowTaskAvailableAction requireTargetAssignee() {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, true, rejectReturnToMeSupported);
    }

    public WorkflowTaskAvailableAction supportRejectReturnToMe(boolean supported) {
        return new WorkflowTaskAvailableAction(actionCode, title, reasonRequired, targetAssigneeRequired, supported);
    }
}
