package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WorkflowSubmitReadFacadeTest {
    private final WorkflowInstanceDao instanceDao = mock(WorkflowInstanceDao.class);
    private final WorkflowDefinitionSelector selector = mock(WorkflowDefinitionSelector.class);
    private final WorkflowSubmitFacade submitFacade = mock(WorkflowSubmitFacade.class);
    private final WorkflowModuleRecordGuard recordGuard = mock(WorkflowModuleRecordGuard.class);
    private final WorkflowSubmitReadFacade facade = new WorkflowSubmitReadFacade(instanceDao, selector, submitFacade,
            List.of(recordGuard));

    @Test
    void shouldReturnCurrentInstanceStatusWhenInstanceExists() {
        WorkflowInstance instance = instance("instance-1");
        when(instanceDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(instance));

        WorkflowSubmitStatusView status = facade.status(WorkflowSubmitRequest.approval("sales.contract", "record-1"));

        assertThat(status.displayStatus()).isEqualTo("PROCESSING");
        assertThat(status.instanceId()).isEqualTo("instance-1");
        assertThat(status.definition().definitionId()).isEqualTo("definition-1");
        verify(recordGuard).beforeSubmit(any(WorkflowSubmitRequest.class));
        verifyNoInteractions(selector, submitFacade);
    }

    @Test
    void shouldReturnUnsubmittedStatusWhenDefinitionCanBeSelected() {
        WorkflowDefinitionSelection selection = selection();
        when(instanceDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of());
        when(selector.select(any(WorkflowSubmitRequest.class))).thenReturn(selection);

        WorkflowSubmitStatusView status = facade.status(WorkflowSubmitRequest.approval("sales.contract", "record-1"));

        assertThat(status.displayStatus()).isEqualTo("UNSUBMITTED");
        assertThat(status.definition().definitionAlias()).isEqualTo("defaultApproval");
        verify(recordGuard).beforeSubmit(any(WorkflowSubmitRequest.class));
    }

    @Test
    void shouldReturnNoWorkflowStatusWhenApprovalDefinitionIsMissing() {
        when(instanceDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of());
        when(selector.select(any(WorkflowSubmitRequest.class)))
                .thenThrow(new PlatformException("published workflow definition not found: sales.contract"));

        WorkflowSubmitStatusView status = facade.status(WorkflowSubmitRequest.approval("sales.contract", "record-1"));

        assertThat(status.displayStatus()).isEqualTo("NO_WORKFLOW");
        assertThat(status.errorMessage()).contains("published workflow definition not found");
    }

    @Test
    void shouldReturnSubmitPreviewViewFromSubmitFacade() {
        WorkflowSubmitPreview preview = new WorkflowSubmitPreview(selection(), draft());
        when(submitFacade.preview(any(WorkflowSubmitRequest.class))).thenReturn(preview);

        WorkflowSubmitPreviewView view = facade.preview(WorkflowSubmitRequest.approval("sales.contract", "record-1")
                .withOperator("user-1"));

        assertThat(view.mode()).isEqualTo("SUBMIT_PREVIEW");
        assertThat(view.definition().definitionAlias()).isEqualTo("defaultApproval");
        assertThat(view.instance().getId()).isEqualTo("instance-1");
    }

    private WorkflowInstance instance(String id) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(id);
        instance.setDefinitionId("definition-1");
        instance.setWorkflowVersionId("version-1");
        instance.setVersionNo(1);
        instance.setModuleAlias("sales.contract");
        instance.setRecordId("record-1");
        instance.setApprovalEnabled(Boolean.TRUE);
        instance.setApprovalStatus(WorkflowApprovalStatus.PROCESSING);
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        instance.setStartedAt(Instant.parse("2026-06-05T01:00:00Z"));
        return instance;
    }

    private WorkflowDefinitionSelection selection() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId("definition-1");
        definition.setAlias("defaultApproval");
        definition.setTitle("Default Approval");
        WorkflowVersion version = new WorkflowVersion();
        version.setId("version-1");
        version.setVersionNo(1);
        return new WorkflowDefinitionSelection(definition, version, List.of(), List.of());
    }

    private WorkflowSubmitDraft draft() {
        return new WorkflowSubmitDraft(instance("instance-1"), List.of(), List.of(), List.of(), List.of(),
                new WorkflowActivationResult(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false));
    }
}
