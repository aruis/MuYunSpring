package net.ximatai.muyun.spring.platform.workflow;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkflowNodeOvertimeScheduler {
    private final WorkflowNodeOvertimeScanService scanService;

    public WorkflowNodeOvertimeScheduler(WorkflowNodeOvertimeScanService scanService) {
        this.scanService = scanService;
    }

    @Scheduled(
            fixedDelayString = "${muyun.workflow.overtime-scan-delay-ms:300000}",
            initialDelayString = "${muyun.workflow.overtime-scan-initial-delay-ms:60000}"
    )
    public void scanScheduled() {
        scanService.scan(null);
    }
}
