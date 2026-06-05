package net.ximatai.muyun.spring.common.model.capability;

import java.time.Instant;

public interface ApprovalCapable {
    String getApprovalInstanceId();

    void setApprovalInstanceId(String approvalInstanceId);

    String getApprovalStatus();

    void setApprovalStatus(String approvalStatus);

    String getApprovalSubmittedBy();

    void setApprovalSubmittedBy(String approvalSubmittedBy);

    Instant getApprovalSubmittedAt();

    void setApprovalSubmittedAt(Instant approvalSubmittedAt);

    Instant getApprovalCompletedAt();

    void setApprovalCompletedAt(Instant approvalCompletedAt);
}
