package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.stereotype.Service;

@Service
public class DynamicWorkflowApprovalSummaryWriter implements WorkflowApprovalSummaryWriter {
    private final DynamicRecordService dynamicRecordService;

    public DynamicWorkflowApprovalSummaryWriter(DynamicRecordService dynamicRecordService) {
        this.dynamicRecordService = dynamicRecordService;
    }

    @Override
    public void writeSubmitted(WorkflowApprovalSummary summary) {
        String entityAlias = dynamicRecordService.mainEntityAlias(summary.moduleAlias());
        DynamicEntityDescriptor entity = dynamicRecordService.entityDescriptor(summary.moduleAlias(), entityAlias);
        if (!entity.capabilities().contains(EntityCapability.APPROVAL.name())) {
            throw new PlatformException("dynamic module does not support approval: " + summary.moduleAlias());
        }
        DynamicRecord record = dynamicRecordService.selectSystem(summary.moduleAlias(), entityAlias, summary.recordId());
        if (record == null) {
            throw new PlatformException("dynamic record not found: " + summary.moduleAlias() + "." + summary.recordId());
        }
        record.setApprovalInstanceId(summary.approvalInstanceId());
        record.setApprovalStatus(summary.approvalStatus().getCode());
        record.setApprovalSubmittedBy(summary.approvalSubmittedBy());
        record.setApprovalSubmittedAt(summary.approvalSubmittedAt());
        record.setApprovalCompletedAt(summary.approvalCompletedAt());
        dynamicRecordService.updateSystem(summary.moduleAlias(), entityAlias, record, "workflow submit");
    }
}
