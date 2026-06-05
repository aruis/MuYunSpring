package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowRuntimeTaskDraft(
        List<WorkflowTask> tasks,
        List<WorkflowEvent> events
) {
    public WorkflowRuntimeTaskDraft {
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
