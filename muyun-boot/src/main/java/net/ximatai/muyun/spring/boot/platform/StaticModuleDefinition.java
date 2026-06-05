package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record StaticModuleDefinition(
        String applicationAlias,
        String moduleAlias,
        String title,
        String parentModuleAlias,
        Set<EntityCapability> capabilities,
        List<StaticModuleActionDefinition> actions
) {
    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  List<StaticModuleActionDefinition> actions) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, Set.of(), actions);
    }

    public StaticModuleDefinition {
        applicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        moduleAlias = PlatformNameRules.requireModuleAliasInApplication(moduleAlias, applicationAlias);
        title = title == null || title.isBlank() ? moduleAlias : title.trim();
        if (parentModuleAlias != null && parentModuleAlias.isBlank()) {
            parentModuleAlias = null;
        }
        if (parentModuleAlias != null) {
            parentModuleAlias = PlatformNameRules.requireModuleAliasInApplication(parentModuleAlias, applicationAlias);
        }
        capabilities = normalizeCapabilities(capabilities);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public boolean supports(EntityCapability capability) {
        return capabilities.contains(capability);
    }

    private static Set<EntityCapability> normalizeCapabilities(Set<EntityCapability> capabilities) {
        if (capabilities == null || capabilities.isEmpty()) {
            return Set.of();
        }
        EnumSet<EntityCapability> normalized = EnumSet.copyOf(capabilities);
        if (normalized.contains(EntityCapability.APPROVAL)) {
            normalized.add(EntityCapability.WORKFLOW);
        }
        return Set.copyOf(normalized);
    }
}
