package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicEntityDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicWorkflowApprovalSummaryWriterTest {
    private final DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
    private final DynamicWorkflowApprovalSummaryWriter writer = new DynamicWorkflowApprovalSummaryWriter(dynamicRecordService);

    @Test
    void shouldWriteApprovalSummaryToDynamicMainEntityBySystemUpdate() {
        DynamicRecord record = new DynamicRecord(entity());
        record.setId("record-1");
        when(dynamicRecordService.mainEntityAlias("sales.contract")).thenReturn("contract");
        when(dynamicRecordService.entityDescriptor("sales.contract", "contract")).thenReturn(descriptor());
        when(dynamicRecordService.selectSystem("sales.contract", "contract", "record-1")).thenReturn(record);

        writer.writeSubmitted(new WorkflowApprovalSummary(
                "sales.contract",
                "record-1",
                "instance-1",
                WorkflowApprovalStatus.PROCESSING,
                "user-1",
                Instant.parse("2026-06-05T01:00:00Z"),
                null
        ));

        assertThat(record.getApprovalInstanceId()).isEqualTo("instance-1");
        assertThat(record.getApprovalStatus()).isEqualTo("processing");
        assertThat(record.getApprovalSubmittedBy()).isEqualTo("user-1");
        assertThat(record.getApprovalSubmittedAt()).isEqualTo(Instant.parse("2026-06-05T01:00:00Z"));
        verify(dynamicRecordService).updateSystem("sales.contract", "contract", record, "workflow submit");
    }

    private EntityDefinition entity() {
        return new EntityDefinition("contract", "app_contract", "Contract",
                List.of(FieldDefinition.string("code", "Code")))
                .withCapabilities(EntityCapability.APPROVAL);
    }

    private DynamicEntityDescriptor descriptor() {
        return new DynamicEntityDescriptor("contract", "Contract",
                Set.of(EntityCapability.APPROVAL.name(), EntityCapability.WORKFLOW.name()),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
