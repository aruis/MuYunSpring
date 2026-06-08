package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowSubmitPreviewView(
        String mode,
        WorkflowDefinitionSummaryView definition,
        WorkflowInstance instance,
        List<WorkflowNodeInstance> nodes,
        List<WorkflowRouteInstance> routes,
        List<WorkflowTask> tasks,
        List<WorkflowEvent> events,
        WorkflowActivationResult activation
) {
    public WorkflowSubmitPreviewView {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        routes = routes == null ? List.of() : List.copyOf(routes);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        events = events == null ? List.of() : List.copyOf(events);
    }

    public static WorkflowSubmitPreviewView of(WorkflowSubmitPreview preview) {
        WorkflowSubmitDraft draft = preview.draft();
        return new WorkflowSubmitPreviewView("SUBMIT_PREVIEW", WorkflowDefinitionSummaryView.of(preview.selection()),
                draft.instance(), draft.nodes(), draft.routes(), draft.tasks(), draft.events(), draft.activation());
    }
}
