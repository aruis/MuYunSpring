package net.ximatai.muyun.spring.platform.workflow;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowRuntimePluginDispatcherTest {
    @Test
    void shouldFilterAndDispatchPluginsInStableOrder() {
        List<String> calls = new ArrayList<>();
        WorkflowRuntimePluginDispatcher dispatcher = new WorkflowRuntimePluginDispatcher(List.of(
                plugin("late", 20, "sales.contract", "approve", calls),
                plugin("early-b", 10, "sales.contract", "approve", calls),
                plugin("early-a", 10, "sales.contract", "approve", calls),
                plugin("other-node", 1, "sales.contract", "review", calls),
                plugin("other-module", 1, "sales.order", "approve", calls)
        ));

        dispatcher.dispatch(new WorkflowRuntimePluginContext(WorkflowRuntimePluginEventType.BEFORE_APPROVE,
                "approve", "sales.contract", "record-1", "instance-1", "approve", "task-1",
                "user-1", null, null, null, "agree", null, null, null));

        assertThat(calls).containsExactly("early-a", "early-b", "late");
    }

    private WorkflowRuntimePlugin plugin(String key, int order, String moduleAlias, String nodeKey,
                                         List<String> calls) {
        return new WorkflowRuntimePlugin() {
            @Override
            public String pluginKey() {
                return key;
            }

            @Override
            public int order() {
                return order;
            }

            @Override
            public Set<WorkflowRuntimePluginEventType> eventTypes() {
                return Set.of(WorkflowRuntimePluginEventType.BEFORE_APPROVE);
            }

            @Override
            public String moduleAlias() {
                return moduleAlias;
            }

            @Override
            public String nodeKey() {
                return nodeKey;
            }

            @Override
            public void handle(WorkflowRuntimePluginContext context) {
                calls.add(key);
            }
        };
    }
}
