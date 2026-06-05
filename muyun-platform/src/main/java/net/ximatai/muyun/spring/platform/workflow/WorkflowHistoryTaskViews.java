package net.ximatai.muyun.spring.platform.workflow;

final class WorkflowHistoryTaskViews {
    private WorkflowHistoryTaskViews() {
    }

    static boolean processedByDelegation(WorkflowTask task) {
        if (task == null || task.getAssignmentKind() != WorkflowAssignmentKind.DELEGATED) {
            return false;
        }
        String actualProcessorId = task.getActualProcessorId();
        if (actualProcessorId == null || actualProcessorId.isBlank()) {
            return false;
        }
        String delegatedToUserId = task.getDelegatedToUserId();
        if (delegatedToUserId != null && !delegatedToUserId.isBlank()) {
            return actualProcessorId.equals(delegatedToUserId);
        }
        String originalAssigneeId = task.getOriginalAssigneeId();
        return originalAssigneeId != null && !originalAssigneeId.isBlank()
                && !actualProcessorId.equals(originalAssigneeId);
    }
}
