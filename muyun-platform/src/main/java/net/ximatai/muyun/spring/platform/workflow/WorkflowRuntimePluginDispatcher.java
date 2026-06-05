package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.ability.TransactionScopeSupport;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class WorkflowRuntimePluginDispatcher {
    private final List<WorkflowRuntimePlugin> plugins;

    public WorkflowRuntimePluginDispatcher(List<WorkflowRuntimePlugin> plugins) {
        this.plugins = plugins == null ? List.of() : List.copyOf(plugins);
    }

    public void dispatch(WorkflowRuntimePluginContext context) {
        if (context == null || plugins.isEmpty()) {
            return;
        }
        List<WorkflowRuntimePlugin> matchedPlugins = plugins.stream()
                .filter(plugin -> plugin.supports(context))
                .sorted(Comparator.comparingInt(WorkflowRuntimePlugin::order)
                        .thenComparing(WorkflowRuntimePlugin::pluginKey,
                                Comparator.nullsLast(String::compareTo)))
                .toList();

        matchedPlugins.stream()
                .filter(plugin -> plugin.dispatchTiming() == WorkflowRuntimePluginDispatchTiming.SYNCHRONOUS)
                .forEach(plugin -> plugin.handle(context));

        if (!isAfterEvent(context.eventType())) {
            return;
        }
        matchedPlugins.stream()
                .filter(plugin -> plugin.dispatchTiming() == WorkflowRuntimePluginDispatchTiming.AFTER_COMMIT)
                .forEach(plugin -> TransactionScopeSupport.afterCommitOrNow(() -> plugin.handle(context)));
    }

    private boolean isAfterEvent(WorkflowRuntimePluginEventType eventType) {
        return eventType.name().startsWith("AFTER_");
    }
}
