package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;
import java.util.List;

public record WorkflowHistorySnapshot(
        int snapshotVersion,
        Instant archivedAt,
        WorkflowArchiveReason archiveReason,
        WorkflowInstance instance,
        List<WorkflowNodeInstance> nodes,
        List<WorkflowRouteInstance> routes,
        List<WorkflowTask> tasks,
        List<WorkflowEvent> events
) {
    public WorkflowHistorySnapshot {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        routes = routes == null ? List.of() : List.copyOf(routes);
        tasks = tasks == null ? List.of() : List.copyOf(tasks);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
