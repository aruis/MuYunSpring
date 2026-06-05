package net.ximatai.muyun.spring.platform.workflow;

import java.util.Set;

public interface WorkflowRuntimePlugin {
    String pluginKey();

    default int order() {
        return 0;
    }

    default WorkflowRuntimePluginDispatchTiming dispatchTiming() {
        return WorkflowRuntimePluginDispatchTiming.SYNCHRONOUS;
    }

    default Set<WorkflowRuntimePluginEventType> eventTypes() {
        return Set.of();
    }

    default String moduleAlias() {
        return null;
    }

    default String nodeKey() {
        return null;
    }

    default boolean supports(WorkflowRuntimePluginContext context) {
        if (context == null) {
            return false;
        }
        Set<WorkflowRuntimePluginEventType> events = eventTypes();
        if (events != null && !events.isEmpty() && !events.contains(context.eventType())) {
            return false;
        }
        String moduleAlias = moduleAlias();
        if (moduleAlias != null && !moduleAlias.isBlank() && !moduleAlias.equals(context.moduleAlias())) {
            return false;
        }
        String nodeKey = nodeKey();
        return nodeKey == null || nodeKey.isBlank() || nodeKey.equals(context.nodeKey());
    }

    void handle(WorkflowRuntimePluginContext context);
}
