package net.ximatai.muyun.spring.platform.workflow;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.Dependent;

@Dependent
public class WorkflowNodeOvertimeScheduler {
    private final WorkflowNodeOvertimeScanService scanService;

    public WorkflowNodeOvertimeScheduler(WorkflowNodeOvertimeScanService scanService) {
        this.scanService = scanService;
    }

    @Scheduled(
            every = "{muyun.workflow.overtime-scan-interval:300s}",
            delayed = "{muyun.workflow.overtime-scan-initial-delay:60s}"
    )
    public void scanScheduled() {
        scanService.scan(null);
    }
}
