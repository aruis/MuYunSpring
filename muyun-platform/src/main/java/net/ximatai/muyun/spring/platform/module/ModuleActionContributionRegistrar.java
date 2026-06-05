package net.ximatai.muyun.spring.platform.module;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
        registerAll(List.of(contribution));
    }

    public void registerAll(List<ModuleActionContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return;
        }
        List<ModuleActionContribution> validContributions = contributions.stream()
                .filter(Objects::nonNull)
                .toList();
        if (validContributions.isEmpty()) {
            return;
        }
        try (TenantContext.Scope ignored = TenantContext.system("register contributed module action")) {
            Map<ContributionSource, List<ModuleActionContribution>> bySource = validContributions.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            contribution -> new ContributionSource(contribution.sourceType(), contribution.sourceId()),
                            LinkedHashMap::new,
                            java.util.stream.Collectors.toList()));
            for (List<ModuleActionContribution> sourceContributions : bySource.values()) {
                sourceContributions.forEach(this::validateContribution);
                disableStaleActions(sourceContributions);
                sourceContributions.forEach(this::upsert);
            }
        }
    }

    private void upsert(ModuleActionContribution contribution) {
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

    public void disableBySource(ModuleActionSourceType sourceType, String sourceId) {
        if (sourceType == null || sourceId == null || sourceId.isBlank()) {
            return;
        }
        try (TenantContext.Scope ignored = TenantContext.system("disable contributed module actions")) {
            for (PlatformModuleAction action : actionService.listBySource(sourceType, sourceId)) {
                if (Boolean.FALSE.equals(action.getEnabled())) {
                    continue;
                }
                action.setEnabled(Boolean.FALSE);
                actionService.update(action);
            }
        }
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

    private void disableStaleActions(List<ModuleActionContribution> contributions) {
        if (contributions == null || contributions.isEmpty()) {
            return;
        }
        ModuleActionContribution first = contributions.getFirst();
        Set<String> currentActionCodes = contributions.stream()
                .map(ModuleActionContribution::actionCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (PlatformModuleAction action : actionService.listBySource(first.sourceType(), first.sourceId())) {
            if (currentActionCodes.contains(action.getActionCode())) {
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

    private record ContributionSource(ModuleActionSourceType sourceType, String sourceId) {
    }
}
