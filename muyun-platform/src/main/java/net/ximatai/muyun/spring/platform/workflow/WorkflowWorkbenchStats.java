package net.ximatai.muyun.spring.platform.workflow;

import java.util.List;

public record WorkflowWorkbenchStats(
        String boardType,
        List<WorkflowWorkbenchStatItem> items
) {
    public WorkflowWorkbenchStats {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
