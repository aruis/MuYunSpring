package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowDefinitionSummaryView(
        String definitionId,
        String definitionAlias,
        String definitionTitle,
        String workflowVersionId,
        Integer versionNo
) {
    static WorkflowDefinitionSummaryView of(WorkflowDefinitionSelection selection) {
        if (selection == null || selection.definition() == null || selection.version() == null) {
            return null;
        }
        return new WorkflowDefinitionSummaryView(
                selection.definition().getId(),
                selection.definition().getAlias(),
                selection.definition().getTitle(),
                selection.version().getId(),
                selection.version().getVersionNo());
    }

    static WorkflowDefinitionSummaryView of(WorkflowInstance instance) {
        if (instance == null) {
            return null;
        }
        return new WorkflowDefinitionSummaryView(
                instance.getDefinitionId(),
                null,
                null,
                instance.getWorkflowVersionId(),
                instance.getVersionNo());
    }
}
