package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record StaticModuleDefinition(
        String applicationAlias,
        String moduleAlias,
        String title,
        String parentModuleAlias,
        ModuleEntryType entryType,
        String entryRoute,
        String entryExternalUrl,
        Set<EntityCapability> capabilities,
        List<StaticModuleActionDefinition> actions,
        List<EntityDefinition> entities,
        ModuleUiDefinition uiDefinition
) {
    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  ModuleEntryType entryType,
                                  String entryRoute,
                                  String entryExternalUrl,
                                  Set<EntityCapability> capabilities,
                                  List<StaticModuleActionDefinition> actions,
                                  List<EntityDefinition> entities) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, entryType, entryRoute, entryExternalUrl,
                capabilities, actions, entities, null);
    }

    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  List<StaticModuleActionDefinition> actions) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, ModuleEntryType.MODULE, null, null,
                Set.of(), actions, List.of(), null);
    }

    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  Set<EntityCapability> capabilities,
                                  List<StaticModuleActionDefinition> actions) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, ModuleEntryType.MODULE, null, null,
                capabilities, actions, List.of(), null);
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
        if (entryType == null) {
            entryType = ModuleEntryType.MODULE;
        }
        if (entryRoute != null) {
            entryRoute = entryRoute.trim();
        }
        if (entryExternalUrl != null) {
            entryExternalUrl = entryExternalUrl.trim();
        }
        capabilities = normalizeCapabilities(capabilities);
        actions = actions == null ? List.of() : List.copyOf(actions);
        entities = entities == null ? List.of() : List.copyOf(entities);
        if (uiDefinition != null && !moduleAlias.equals(uiDefinition.moduleAlias())) {
            throw new IllegalArgumentException("static module UI definition alias must match module alias: "
                    + moduleAlias + " != " + uiDefinition.moduleAlias());
        }
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
