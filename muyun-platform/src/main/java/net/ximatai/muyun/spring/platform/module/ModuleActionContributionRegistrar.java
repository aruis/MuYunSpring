package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class ModuleActionContributionRegistrar {
    private final PlatformModuleActionService actionService;

    public ModuleActionContributionRegistrar(PlatformModuleActionService actionService) {
        this.actionService = actionService;
    }

    public void register(ModuleActionContribution contribution) {
        if (contribution == null) {
            return;
        }
        try (TenantContext.Scope ignored = TenantContext.system("register contributed module action")) {
            validateContribution(contribution);
            disableStaleActions(contribution);
            PlatformModuleAction action = actionService.findByModuleAliasAndActionCode(
                    contribution.moduleAlias(), contribution.actionCode());
            if (action == null) {
                action = new PlatformModuleAction();
                action.setModuleAlias(contribution.moduleAlias());
                action.setActionCode(contribution.actionCode());
            } else if (!sameContribution(action, contribution)) {
                throw new PlatformException("module action contribution conflicts with existing action: "
                        + contribution.moduleAlias() + "." + contribution.actionCode());
            }
            apply(action, contribution);
            if (action.getId() == null || action.getId().isBlank()) {
                actionService.insert(action);
            } else {
                actionService.update(action);
            }
        }
    }

    public void registerAll(List<ModuleActionContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return;
        }
        contributions.forEach(this::register);
    }

    private void validateContribution(ModuleActionContribution contribution) {
        if (contribution.sourceType() == null || contribution.sourceId() == null || contribution.sourceId().isBlank()) {
            throw new PlatformException("module action contribution source must not be blank: "
                    + contribution.moduleAlias() + "." + contribution.actionCode());
        }
        if (contribution.bindingType() != null
                && (contribution.bindingId() == null || contribution.bindingId().isBlank()
                || contribution.bindingAlias() == null || contribution.bindingAlias().isBlank())) {
            throw new PlatformException("module action contribution binding must not be blank: "
                    + contribution.moduleAlias() + "." + contribution.actionCode());
        }
    }

    private void disableStaleActions(ModuleActionContribution contribution) {
        for (PlatformModuleAction action : actionService.listBySource(contribution.sourceType(), contribution.sourceId())) {
            if (Objects.equals(action.getActionCode(), contribution.actionCode())) {
                continue;
            }
            action.setEnabled(Boolean.FALSE);
            actionService.update(action);
        }
    }

    private boolean sameContribution(PlatformModuleAction action, ModuleActionContribution contribution) {
        return Boolean.TRUE.equals(action.getSystemManaged())
                && Objects.equals(action.getSourceType(), contribution.sourceType())
                && Objects.equals(action.getSourceId(), contribution.sourceId())
                && Objects.equals(action.getBindingType(), contribution.bindingType())
                && Objects.equals(action.getBindingId(), contribution.bindingId());
    }

    private void apply(PlatformModuleAction action, ModuleActionContribution contribution) {
        action.setEntityAlias(contribution.entityAlias());
        action.setPermissionActionCode(contribution.permissionActionCode());
        action.setTitle(contribution.title());
        action.setCategory(contribution.category());
        action.setActionLevel(contribution.actionLevel());
        action.setAccessMode(contribution.accessMode());
        action.setActionAuth(contribution.actionAuth());
        action.setDataAuth(contribution.dataAuth());
        action.setDefaultGrantPolicy(contribution.defaultGrantPolicy());
        action.setAvailableExpression(contribution.availableExpression());
        action.setUnavailableMessage(contribution.unavailableMessage());
        action.setExecutorType(contribution.executorType());
        action.setExecutorKey(contribution.executorKey());
        action.setSourceType(contribution.sourceType());
        action.setSourceId(contribution.sourceId());
        action.setSourceVersionId(contribution.sourceVersionId());
        action.setBindingType(contribution.bindingType());
        action.setBindingId(contribution.bindingId());
        action.setBindingAlias(contribution.bindingAlias());
        action.setSystemManaged(Boolean.TRUE);
        action.setEnabled(contribution.enabled());
    }
}
