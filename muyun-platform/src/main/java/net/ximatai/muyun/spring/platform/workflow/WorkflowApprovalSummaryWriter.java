package net.ximatai.muyun.spring.platform.workflow;

public interface WorkflowApprovalSummaryWriter {
    void writeSubmitted(WorkflowApprovalSummary summary);

    default void clearCurrent(String moduleAlias, String recordId) {
    }
}
