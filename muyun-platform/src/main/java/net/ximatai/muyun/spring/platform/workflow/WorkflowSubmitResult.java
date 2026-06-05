package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowSubmitResult(
        WorkflowSubmitDraft draft,
        boolean approvalSummaryWritten
) {
    public WorkflowInstance instance() {
        return draft.instance();
    }
}
