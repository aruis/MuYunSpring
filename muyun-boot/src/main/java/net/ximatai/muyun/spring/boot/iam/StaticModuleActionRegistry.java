package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class StaticModuleActionRegistry {
    private final Map<String, Set<PlatformAction>> moduleActions;

    public StaticModuleActionRegistry() {
        this(Map.of());
    }

    public StaticModuleActionRegistry(Map<String, ? extends Collection<PlatformAction>> moduleActions) {
        if (moduleActions == null || moduleActions.isEmpty()) {
            this.moduleActions = Map.of();
            return;
        }
        this.moduleActions = moduleActions.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        entry -> PlatformNameRules.requireModuleAlias(entry.getKey()),
                        entry -> actions(entry.getValue())
                ));
    }

    public List<PlatformAction> grantableActions(String moduleAlias) {
        Optional<Set<PlatformAction>> declared = declaredActions(moduleAlias);
        return java.util.Arrays.stream(PlatformAction.values())
                .filter(PlatformAction::actionAuth)
                .filter(action -> declared.map(actions -> actions.contains(action)).orElse(true))
                .toList();
    }

    public boolean isGrantable(String moduleAlias, PlatformAction action) {
        if (action == null || !action.actionAuth()) {
            return false;
        }
        return declaredActions(moduleAlias)
                .map(actions -> actions.contains(action))
                .orElse(true);
    }

    private Optional<Set<PlatformAction>> declaredActions(String moduleAlias) {
        return Optional.ofNullable(moduleActions.get(PlatformNameRules.requireModuleAlias(moduleAlias)));
    }

    private static Set<PlatformAction> actions(Collection<PlatformAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return Set.of();
        }
        EnumSet<PlatformAction> copied = actions.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PlatformAction.class)));
        return copied.isEmpty() ? Set.of() : Set.copyOf(copied);
    }
}
