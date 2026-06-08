package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowSubmitPreview(
        WorkflowDefinitionSelection selection,
        WorkflowSubmitDraft draft
) {
}
