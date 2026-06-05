package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowAddSignSegment(
        List<WorkflowNodeDefinition> nodeDefinitions,
        List<WorkflowLinkDefinition> linkDefinitions
) {
    public WorkflowAddSignSegment {
        nodeDefinitions = nodeDefinitions == null ? List.of() : List.copyOf(nodeDefinitions);
        linkDefinitions = linkDefinitions == null ? List.of() : List.copyOf(linkDefinitions);
    }
}
