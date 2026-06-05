package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowTaskActionResult(
        WorkflowTask task,
        WorkflowTask createdTask,
        WorkflowNodeInstance node,
        WorkflowInstance instance,
        WorkflowEvent event,
        WorkflowAddSignEditMode addSignEditMode,
        String sourceNodeKey,
        java.util.List<String> addedNodeKeys,
        java.util.List<String> replacedRouteIds
) {
    public WorkflowTaskActionResult {
        addedNodeKeys = addedNodeKeys == null ? java.util.List.of() : java.util.List.copyOf(addedNodeKeys);
        replacedRouteIds = replacedRouteIds == null ? java.util.List.of() : java.util.List.copyOf(replacedRouteIds);
    }

    public WorkflowTaskActionResult(WorkflowTask task,
                                    WorkflowTask createdTask,
                                    WorkflowNodeInstance node,
                                    WorkflowInstance instance,
                                    WorkflowEvent event) {
        this(task, createdTask, node, instance, event, null, null, java.util.List.of(), java.util.List.of());
    }

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

    public static WorkflowTaskActionResult addSign(WorkflowTask task, WorkflowNodeInstance node,
                                                   WorkflowInstance instance, WorkflowEvent event,
                                                   WorkflowAddSignEditMode editMode,
                                                   java.util.List<String> addedNodeKeys,
                                                   java.util.List<String> replacedRouteIds) {
        return new WorkflowTaskActionResult(task, null, node, instance, event, editMode,
                node == null ? null : node.getNodeKey(), addedNodeKeys, replacedRouteIds);
    }
}
