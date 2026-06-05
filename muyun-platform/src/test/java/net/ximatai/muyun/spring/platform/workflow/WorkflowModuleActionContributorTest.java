package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.module.ModuleActionBindingType;
import net.ximatai.muyun.spring.platform.module.ModuleActionContribution;
import net.ximatai.muyun.spring.platform.module.ModuleActionContributionRegistrar;
import net.ximatai.muyun.spring.platform.module.ModuleActionSourceType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WorkflowModuleActionContributorTest {
    private final ModuleActionContributionRegistrar registrar = mock(ModuleActionContributionRegistrar.class);
    private final WorkflowModuleActionContributor contributor = new WorkflowModuleActionContributor(registrar);

    @Test
    void shouldContributeActionForNonApprovalWorkflow() {
        WorkflowDefinition definition = definition(false);
        WorkflowVersion version = version();

        ModuleActionContribution contribution = contributor.contribution(definition, version);

        assertThat(contribution).isNotNull();
        assertThat(contribution.moduleAlias()).isEqualTo("sales.contract");
        assertThat(contribution.actionCode()).isEqualTo("syncWorkflow");
        assertThat(contribution.title()).isEqualTo("同步流程");
        assertThat(contribution.sourceType()).isEqualTo(ModuleActionSourceType.WORKFLOW_DEFINITION);
        assertThat(contribution.sourceId()).isEqualTo("def-1");
        assertThat(contribution.sourceVersionId()).isEqualTo("ver-1");
        assertThat(contribution.bindingType()).isEqualTo(ModuleActionBindingType.WORKFLOW_DEFINITION);
        assertThat(contribution.bindingId()).isEqualTo("def-1");
        assertThat(contribution.bindingAlias()).isEqualTo("sync");
    }

    @Test
    void shouldNotContributeActionForApprovalWorkflow() {
        assertThat(contributor.contribution(definition(true), version())).isNull();
    }

    @Test
    void shouldRejectNonApprovalWorkflowWithoutActionCode() {
        WorkflowDefinition definition = definition(false);
        definition.setActionCode(null);

        assertThatThrownBy(() -> contributor.contribution(definition, version()))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("non-approval workflow actionCode must not be blank");
    }

    @Test
    void shouldRegisterPublishedWorkflowAction() {
        WorkflowDefinition definition = definition(false);
        WorkflowVersion version = version();

        contributor.registerPublishedWorkflowAction(definition, version);

        verify(registrar).register(org.mockito.ArgumentMatchers.argThat(contribution ->
                "syncWorkflow".equals(contribution.actionCode())
                        && "sync".equals(contribution.bindingAlias())));
    }

    private WorkflowDefinition definition(boolean approvalEnabled) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId("def-1");
        definition.setApplicationAlias("sales");
        definition.setModuleAlias("sales.contract");
        definition.setAlias("sync");
        definition.setTitle("同步流程");
        definition.setApprovalEnabled(approvalEnabled);
        definition.setActionCode("syncWorkflow");
        return definition;
    }

    private WorkflowVersion version() {
        WorkflowVersion version = new WorkflowVersion();
        version.setId("ver-1");
        version.setDefinitionId("def-1");
        version.setVersionNo(1);
        version.setPublishStatus(WorkflowPublishStatus.PUBLISHED);
        return version;
    }
}
