package net.ximatai.muyun.spring.platform.workflow;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorkflowModuleSubmitServiceTest {
    private final WorkflowSubmitFacade submitFacade = mock(WorkflowSubmitFacade.class);
    private final WorkflowModuleSubmitService service = new WorkflowModuleSubmitService(submitFacade);

    @Test
    void shouldSubmitApprovalThroughPlatformFacade() {
        service.submitApproval("sales.contract", "record-1");

        verify(submitFacade).submit(WorkflowSubmitRequest.approval("sales.contract", "record-1"));
    }

    @Test
    void shouldSubmitWorkflowByDefinitionAliasThroughPlatformFacade() {
        service.submitWorkflow("sales.contract", "record-1", "sync");

        verify(submitFacade).submit(WorkflowSubmitRequest.workflow("sales.contract", "record-1", "sync"));
    }
}
