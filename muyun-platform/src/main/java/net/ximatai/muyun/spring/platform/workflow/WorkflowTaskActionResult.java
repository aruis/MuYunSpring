package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowTaskActionResult(
        WorkflowTask task,
        WorkflowTask createdTask,
        WorkflowEvent event
) {
    public static WorkflowTaskActionResult of(WorkflowTask task, WorkflowEvent event) {
        return new WorkflowTaskActionResult(task, null, event);
    }

    public static WorkflowTaskActionResult transferred(WorkflowTask task, WorkflowTask createdTask,
                                                       WorkflowEvent event) {
        return new WorkflowTaskActionResult(task, createdTask, event);
    }
}
