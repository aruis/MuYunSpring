package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class WorkflowVersionServiceTest {
    private final WorkflowDefinitionService definitionService = new WorkflowDefinitionService(new TestMemoryDao<>());
    private final WorkflowModuleActionContributor actionContributor = mock(WorkflowModuleActionContributor.class);
    private final WorkflowVersionService service = new WorkflowVersionService(new TestMemoryDao<>(),
            definitionService, actionContributor);

    @Test
    void shouldRegisterActionWhenVersionIsPublished() {
        WorkflowDefinition definition = definition();
        definitionService.insert(definition);
        WorkflowVersion version = version(definition.getId(), WorkflowPublishStatus.PUBLISHED);

        service.insert(version);

        verify(actionContributor).registerPublishedWorkflowAction(definition, version);
    }

    @Test
    void shouldNotRegisterActionWhenVersionIsDraft() {
        WorkflowDefinition definition = definition();
        definitionService.insert(definition);
        WorkflowVersion version = version(definition.getId(), WorkflowPublishStatus.DRAFT);

        service.insert(version);

        verifyNoInteractions(actionContributor);
    }

    private WorkflowDefinition definition() {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId("def-1");
        definition.setApplicationAlias("sales");
        definition.setModuleAlias("sales.contract");
        definition.setAlias("sync");
        definition.setTitle("同步流程");
        definition.setApprovalEnabled(false);
        definition.setActionCode("syncWorkflow");
        return definition;
    }

    private WorkflowVersion version(String definitionId, WorkflowPublishStatus status) {
        WorkflowVersion version = new WorkflowVersion();
        version.setId("ver-1");
        version.setDefinitionId(definitionId);
        version.setVersionNo(1);
        version.setPublishStatus(status);
        return version;
    }
}
