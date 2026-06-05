package net.ximatai.muyun.spring.common.model.standard;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.capability.ApprovalCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

import java.time.Instant;

@Getter
@Setter
public abstract class StandardApprovalEntity extends StandardEntity implements ApprovalCapable {
    @Column(name = PlatformAbilityFields.APPROVAL_INSTANCE_COLUMN,
            type = ColumnType.VARCHAR,
            length = PlatformAbilityFields.APPROVAL_INSTANCE_LENGTH,
            comment = "Current approval workflow instance id")
    private String approvalInstanceId;

    @Column(name = PlatformAbilityFields.APPROVAL_STATUS_COLUMN,
            type = ColumnType.VARCHAR,
            length = PlatformAbilityFields.APPROVAL_STATUS_LENGTH,
            comment = "Current approval status")
    private String approvalStatus;

    @Column(name = PlatformAbilityFields.APPROVAL_SUBMITTED_BY_COLUMN,
            type = ColumnType.VARCHAR,
            length = PlatformAbilityFields.APPROVAL_SUBMITTED_BY_LENGTH,
            comment = "Approval submitted by")
    private String approvalSubmittedBy;

    @Column(name = PlatformAbilityFields.APPROVAL_SUBMITTED_AT_COLUMN,
            type = ColumnType.TIMESTAMP,
            comment = "Approval submitted at")
    private Instant approvalSubmittedAt;

    @Column(name = PlatformAbilityFields.APPROVAL_COMPLETED_AT_COLUMN,
            type = ColumnType.TIMESTAMP,
            comment = "Approval completed at")
    private Instant approvalCompletedAt;
}
