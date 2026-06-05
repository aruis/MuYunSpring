package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.platform.module.ModuleActionBindingType;
import net.ximatai.muyun.spring.platform.module.ModuleActionContributionRegistrar;
import net.ximatai.muyun.spring.platform.module.ModuleActionSourceType;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowPublishFacadeTest {
    private final TestMemoryDao<PlatformModule> moduleDao = new TestMemoryDao<>();
    private final TestMemoryDao<PlatformModuleAction> actionDao = new TestMemoryDao<>();
    private final WorkflowDefinitionService definitionService = new WorkflowDefinitionService(new TestMemoryDao<>());
    private final WorkflowVersionService versionService = new WorkflowVersionService(new TestMemoryDao<>(),
            definitionService);
    private final PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
    private final PlatformModuleActionService actionService = new PlatformModuleActionService(actionDao, moduleService);
    private final WorkflowPublishFacade facade = new WorkflowPublishFacade(
            definitionService,
            versionService,
            new WorkflowModuleActionContributor(new ModuleActionContributionRegistrar(actionService)));

    @Test
    void shouldPublishWorkflowVersionAndContributeModuleAction() {
        moduleService.insert(module("sales.contract"));
        WorkflowDefinition definition = definition();
        definitionService.insert(definition);
        WorkflowVersion version = version(definition.getId(), 1, WorkflowPublishStatus.DRAFT);
        versionService.insert(version);

        facade.publish(definition.getId(), version.getId(), "manager-1");

        WorkflowDefinition publishedDefinition = definitionService.select(definition.getId());
        WorkflowVersion publishedVersion = versionService.select(version.getId());
        PlatformModuleAction action = actionService.findByModuleAliasAndActionCode("sales.contract", "syncWorkflow");
        assertThat(publishedDefinition.getDefinitionStatus()).isEqualTo(WorkflowDefinitionStatus.PUBLISHED);
        assertThat(publishedDefinition.getCurrentVersionNo()).isEqualTo(1);
        assertThat(publishedVersion.getPublishStatus()).isEqualTo(WorkflowPublishStatus.PUBLISHED);
        assertThat(publishedVersion.getPublishedBy()).isEqualTo("manager-1");
        assertThat(publishedVersion.getPublishedAt()).isNotNull();
        assertThat(action).isNotNull();
        assertThat(action.getCategory()).isEqualTo(EntityActionCategory.WORKFLOW);
        assertThat(action.getBindingType()).isEqualTo(ModuleActionBindingType.WORKFLOW_DEFINITION);
        assertThat(action.getBindingAlias()).isEqualTo("sync");
    }

    @Test
    void shouldPublishApprovalWorkflowRuntimeActionsWithoutSubmitWorkflowAction() {
        moduleService.insert(module("sales.contract"));
        WorkflowDefinition definition = definition();
        definition.setApprovalEnabled(true);
        definition.setActionCode(null);
        definitionService.insert(definition);
        WorkflowVersion version = version(definition.getId(), 1, WorkflowPublishStatus.DRAFT);
        versionService.insert(version);

        facade.publish(definition.getId(), version.getId());

        assertThat(actionService.findByModuleAliasAndActionCode("sales.contract", "syncWorkflow")).isNull();
        PlatformModuleAction approve = actionService.findByModuleAliasAndActionCode("sales.contract", "approve");
        assertThat(approve).isNotNull();
        assertThat(approve.getPermissionActionCode()).isEqualTo("approve");
        assertThat(approve.getActionLevel()).isEqualTo(EntityActionLevel.RECORD);
        assertThat(approve.getActionAuth()).isTrue();
        assertThat(approve.getDataAuth()).isTrue();
        assertThat(approve.getSourceType()).isEqualTo(ModuleActionSourceType.WORKFLOW_RUNTIME);
    }

    @Test
    void shouldDisableContributedWorkflowActionWhenDefinitionIsDisabled() {
        moduleService.insert(module("sales.contract"));
        WorkflowDefinition definition = definition();
        definitionService.insert(definition);
        WorkflowVersion version = version(definition.getId(), 1, WorkflowPublishStatus.DRAFT);
        versionService.insert(version);
        facade.publish(definition.getId(), version.getId());

        facade.disable(definition.getId());

        WorkflowDefinition disabledDefinition = definitionService.select(definition.getId());
        PlatformModuleAction action = actionService.findByModuleAliasAndActionCode("sales.contract", "syncWorkflow");
        assertThat(disabledDefinition.getDefinitionStatus()).isEqualTo(WorkflowDefinitionStatus.DISABLED);
        assertThat(action.getEnabled()).isFalse();
    }

    @Test
    void shouldDisableContributedWorkflowActionWhenDefinitionIsArchived() {
        moduleService.insert(module("sales.contract"));
        WorkflowDefinition definition = definition();
        definitionService.insert(definition);
        WorkflowVersion version = version(definition.getId(), 1, WorkflowPublishStatus.DRAFT);
        versionService.insert(version);
        facade.publish(definition.getId(), version.getId());

        facade.archive(definition.getId());

        WorkflowDefinition archivedDefinition = definitionService.select(definition.getId());
        PlatformModuleAction action = actionService.findByModuleAliasAndActionCode("sales.contract", "syncWorkflow");
        assertThat(archivedDefinition.getDefinitionStatus()).isEqualTo(WorkflowDefinitionStatus.ARCHIVED);
        assertThat(action.getEnabled()).isFalse();
    }

    @Test
    void shouldReEnableContributedWorkflowActionWhenDefinitionIsRepublished() {
        moduleService.insert(module("sales.contract"));
        WorkflowDefinition definition = definition();
        definitionService.insert(definition);
        WorkflowVersion version = version(definition.getId(), 1, WorkflowPublishStatus.DRAFT);
        versionService.insert(version);
        facade.publish(definition.getId(), version.getId());
        facade.disable(definition.getId());

        facade.publish(definition.getId(), version.getId());

        WorkflowDefinition publishedDefinition = definitionService.select(definition.getId());
        PlatformModuleAction action = actionService.findByModuleAliasAndActionCode("sales.contract", "syncWorkflow");
        assertThat(publishedDefinition.getDefinitionStatus()).isEqualTo(WorkflowDefinitionStatus.PUBLISHED);
        assertThat(action.getEnabled()).isTrue();
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

    private WorkflowVersion version(String definitionId, int versionNo, WorkflowPublishStatus status) {
        WorkflowVersion version = new WorkflowVersion();
        version.setId("ver-" + versionNo);
        version.setDefinitionId(definitionId);
        version.setVersionNo(versionNo);
        version.setPublishStatus(status);
        return version;
    }

    private PlatformModule module(String alias) {
        PlatformModule module = new PlatformModule();
        module.setAlias(alias);
        module.setApplicationAlias(alias.substring(0, alias.indexOf('.')));
        module.setTitle(alias);
        return module;
    }
}
