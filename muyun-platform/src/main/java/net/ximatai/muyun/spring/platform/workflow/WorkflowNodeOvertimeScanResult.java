package net.ximatai.muyun.spring.platform.workflow;

public record WorkflowNodeOvertimeScanResult(
        int scannedCount,
        int warnedCount,
        int overdueCount
) {
    public int updatedCount() {
        return warnedCount + overdueCount;
    }
}
