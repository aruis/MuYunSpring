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
        requireApprovalEntity(summary.moduleAlias(), entityAlias);
        DynamicRecord record = requireRecord(summary.moduleAlias(), entityAlias, summary.recordId());
        record.setApprovalInstanceId(summary.approvalInstanceId());
        record.setApprovalStatus(summary.approvalStatus().getCode());
        record.setApprovalSubmittedBy(summary.approvalSubmittedBy());
        record.setApprovalSubmittedAt(summary.approvalSubmittedAt());
        record.setApprovalCompletedAt(summary.approvalCompletedAt());
        dynamicRecordService.updateSystem(summary.moduleAlias(), entityAlias, record, "workflow submit");
    }

    @Override
    public void clearCurrent(String moduleAlias, String recordId) {
        String entityAlias = dynamicRecordService.mainEntityAlias(moduleAlias);
        requireApprovalEntity(moduleAlias, entityAlias);
        DynamicRecord record = requireRecord(moduleAlias, entityAlias, recordId);
        record.setApprovalInstanceId(null);
        record.setApprovalStatus(null);
        record.setApprovalSubmittedBy(null);
        record.setApprovalSubmittedAt(null);
        record.setApprovalCompletedAt(null);
        dynamicRecordService.updateSystem(moduleAlias, entityAlias, record, "workflow archive");
    }

    private void requireApprovalEntity(String moduleAlias, String entityAlias) {
        DynamicEntityDescriptor entity = dynamicRecordService.entityDescriptor(moduleAlias, entityAlias);
        if (!entity.capabilities().contains(EntityCapability.APPROVAL.name())) {
            throw new PlatformException("dynamic module does not support approval: " + moduleAlias);
        }
    }

    private DynamicRecord requireRecord(String moduleAlias, String entityAlias, String recordId) {
        DynamicRecord record = dynamicRecordService.selectSystem(moduleAlias, entityAlias, recordId);
        if (record == null) {
            throw new PlatformException("dynamic record not found: " + moduleAlias + "." + recordId);
        }
        return record;
    }
}
