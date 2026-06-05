package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowTaskActionResult(
        WorkflowTask task,
        WorkflowTask createdTask,
        WorkflowNodeInstance node,
        WorkflowInstance instance,
        WorkflowEvent event
) {
    public static WorkflowTaskActionResult of(WorkflowTask task, WorkflowEvent event) {
        return new WorkflowTaskActionResult(task, null, null, null, event);
    }

    public static WorkflowTaskActionResult of(WorkflowTask task, WorkflowNodeInstance node,
                                              WorkflowInstance instance, WorkflowEvent event) {
        return new WorkflowTaskActionResult(task, null, node, instance, event);
    }

    public static WorkflowTaskActionResult transferred(WorkflowTask task, WorkflowTask createdTask,
                                                       WorkflowEvent event) {
        return new WorkflowTaskActionResult(task, createdTask, null, null, event);
    }
}
