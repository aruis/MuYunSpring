package net.ximatai.muyun.spring.platform.workflow;

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
        plugins.stream()
                .filter(plugin -> plugin.supports(context))
                .sorted(Comparator.comparingInt(WorkflowRuntimePlugin::order)
                        .thenComparing(WorkflowRuntimePlugin::pluginKey,
                                Comparator.nullsLast(String::compareTo)))
                .forEach(plugin -> plugin.handle(context));
    }
}
