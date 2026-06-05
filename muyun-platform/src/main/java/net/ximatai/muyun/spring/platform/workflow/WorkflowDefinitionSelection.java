package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowDefinitionSelection(
        WorkflowDefinition definition,
        WorkflowVersion version,
        List<WorkflowNodeDefinition> nodes,
        List<WorkflowLinkDefinition> links
) {
    public WorkflowDefinitionSelection {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        links = links == null ? List.of() : List.copyOf(links);
    }
}
