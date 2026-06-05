package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionAccessMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionCategory;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionExecutorType;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.platform.module.ModuleActionBindingType;
import net.ximatai.muyun.spring.platform.module.ModuleActionContribution;
import net.ximatai.muyun.spring.platform.module.ModuleActionContributionRegistrar;
import net.ximatai.muyun.spring.platform.module.ModuleActionSourceType;
import org.springframework.stereotype.Service;

@Service
public class WorkflowModuleActionContributor {
    private final ModuleActionContributionRegistrar actionRegistrar;

    public WorkflowModuleActionContributor(ModuleActionContributionRegistrar actionRegistrar) {
        this.actionRegistrar = actionRegistrar;
    }

    public void registerPublishedWorkflowAction(WorkflowDefinition definition, WorkflowVersion version) {
        ModuleActionContribution contribution = contribution(definition, version);
        if (contribution != null) {
            actionRegistrar.register(contribution);
        }
    }

    public ModuleActionContribution contribution(WorkflowDefinition definition, WorkflowVersion version) {
        if (definition == null || Boolean.TRUE.equals(definition.getApprovalEnabled())) {
            return null;
        }
        String actionCode = definition.getActionCode();
        if (actionCode == null || actionCode.isBlank()) {
            throw new PlatformException("non-approval workflow actionCode must not be blank: "
                    + definition.getModuleAlias() + "." + definition.getAlias());
        }
        String actionTitle = definition.getTitle() == null || definition.getTitle().isBlank()
                ? actionCode
                : definition.getTitle();
        return new ModuleActionContribution(
                definition.getModuleAlias(),
                null,
                PlatformNameRules.requireActionCode(actionCode, "actionCode"),
                actionCode,
                actionTitle,
                EntityActionCategory.WORKFLOW,
                EntityActionLevel.RECORD,
                EntityActionAccessMode.AUTH_REQUIRED,
                true,
                false,
                ActionDefaultGrantPolicy.NONE,
                null,
                null,
                EntityActionExecutorType.SERVICE,
                DynamicWorkflowActionExecutor.EXECUTOR_KEY,
                ModuleActionSourceType.WORKFLOW_DEFINITION,
                definition.getId(),
                version == null ? null : version.getId(),
                ModuleActionBindingType.WORKFLOW_DEFINITION,
                definition.getId(),
                definition.getAlias(),
                true
        );
    }
}
