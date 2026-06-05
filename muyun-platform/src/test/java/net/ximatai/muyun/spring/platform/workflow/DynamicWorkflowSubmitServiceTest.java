package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecord;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DynamicWorkflowSubmitServiceTest {
    private final DynamicRecordService dynamicRecordService = mock(DynamicRecordService.class);
    private final WorkflowSubmitFacade submitFacade = mock(WorkflowSubmitFacade.class);
    private final DynamicWorkflowSubmitService service = new DynamicWorkflowSubmitService(dynamicRecordService, submitFacade);

    @Test
    void shouldSubmitDynamicApprovalThroughPlatformFacade() {
        DynamicRecord record = record("record-1");
        when(dynamicRecordService.mainEntityAlias("sales.contract")).thenReturn("contract");
        when(dynamicRecordService.selectSystem("sales.contract", "contract", "record-1")).thenReturn(record);

        service.submitApproval("sales.contract", "record-1");

        verify(dynamicRecordService).requireRecordActionScope(eq("sales.contract"), eq("contract"), any(),
                eq(Set.of("record-1")), eq(Optional.empty()));
        verify(submitFacade).submit(WorkflowSubmitRequest.approval("sales.contract", "record-1"));
    }

    @Test
    void shouldSubmitDynamicWorkflowByDefinitionAliasThroughPlatformFacade() {
        DynamicRecord record = record("record-1");
        when(dynamicRecordService.mainEntityAlias("sales.contract")).thenReturn("contract");
        when(dynamicRecordService.selectSystem("sales.contract", "contract", "record-1")).thenReturn(record);

        service.submitWorkflow("sales.contract", "record-1", "sync");

        verify(submitFacade).submit(WorkflowSubmitRequest.workflow("sales.contract", "record-1", "sync"));
    }

    @Test
    void shouldRejectMissingDynamicRecordBeforeStartingWorkflow() {
        when(dynamicRecordService.mainEntityAlias("sales.contract")).thenReturn("contract");
        when(dynamicRecordService.selectSystem("sales.contract", "contract", "missing")).thenReturn(null);

        assertThatThrownBy(() -> service.submitApproval("sales.contract", "missing"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("dynamic record not found");
    }

    private DynamicRecord record(String id) {
        DynamicRecord record = new DynamicRecord(new EntityDefinition("contract", "app_contract", "Contract",
                List.of(FieldDefinition.string("code", "Code"))));
        record.setId(id);
        return record;
    }
}
