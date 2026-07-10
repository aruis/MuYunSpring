package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
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
        ModuleUiDefinition uiDefinition,
        List<StaticModuleReferenceDefinition> references,
        List<StaticModuleReadProjectionDefinition> readProjections,
        Class<?> modelClass,
        List<RelationProjectionJoinDefinition> projectionJoins
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
                capabilities, actions, entities, null, List.of(), List.of(), List.of());
    }

    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  List<StaticModuleActionDefinition> actions) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, ModuleEntryType.MODULE, null, null,
                Set.of(), actions, List.of(), null, List.of(), List.of(), List.of());
    }

    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  Set<EntityCapability> capabilities,
                                  List<StaticModuleActionDefinition> actions) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, ModuleEntryType.MODULE, null, null,
                capabilities, actions, List.of(), null, List.of(), List.of(), List.of());
    }

    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  ModuleEntryType entryType,
                                  String entryRoute,
                                  String entryExternalUrl,
                                  Set<EntityCapability> capabilities,
        List<StaticModuleActionDefinition> actions,
        List<EntityDefinition> entities,
        ModuleUiDefinition uiDefinition) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, entryType, entryRoute, entryExternalUrl,
                capabilities, actions, entities, uiDefinition, List.of(), List.of(), List.of());
    }

    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  ModuleEntryType entryType,
                                  String entryRoute,
                                  String entryExternalUrl,
                                  Set<EntityCapability> capabilities,
                                  List<StaticModuleActionDefinition> actions,
        List<EntityDefinition> entities,
        ModuleUiDefinition uiDefinition,
        List<RelationProjectionJoinDefinition> projectionJoins) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, entryType, entryRoute, entryExternalUrl,
                capabilities, actions, entities, uiDefinition, List.of(), List.of(), null, projectionJoins);
    }

    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  ModuleEntryType entryType,
                                  String entryRoute,
                                  String entryExternalUrl,
                                  Set<EntityCapability> capabilities,
                                  List<StaticModuleActionDefinition> actions,
                                  List<EntityDefinition> entities,
                                  ModuleUiDefinition uiDefinition,
                                  List<StaticModuleReferenceDefinition> references,
                                  List<StaticModuleReadProjectionDefinition> readProjections) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, entryType, entryRoute, entryExternalUrl,
                capabilities, actions, entities, uiDefinition, references, readProjections, null, List.of());
    }

    public StaticModuleDefinition(String applicationAlias,
                                  String moduleAlias,
                                  String title,
                                  String parentModuleAlias,
                                  ModuleEntryType entryType,
                                  String entryRoute,
                                  String entryExternalUrl,
                                  Set<EntityCapability> capabilities,
                                  List<StaticModuleActionDefinition> actions,
                                  List<EntityDefinition> entities,
                                  ModuleUiDefinition uiDefinition,
                                  List<StaticModuleReferenceDefinition> references,
                                  List<StaticModuleReadProjectionDefinition> readProjections,
                                  List<RelationProjectionJoinDefinition> projectionJoins) {
        this(applicationAlias, moduleAlias, title, parentModuleAlias, entryType, entryRoute, entryExternalUrl,
                capabilities, actions, entities, uiDefinition, references, readProjections, null, projectionJoins);
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
        references = references == null ? List.of() : List.copyOf(references);
        readProjections = readProjections == null ? List.of() : List.copyOf(readProjections);
        projectionJoins = projectionJoins == null ? List.of() : List.copyOf(projectionJoins);
        validateReferences(moduleAlias, references);
        validateReadProjectionOutputFields(moduleAlias, entities, readProjections);
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

    private static void validateReadProjectionOutputFields(String moduleAlias,
                                                           List<EntityDefinition> entities,
                                                           List<StaticModuleReadProjectionDefinition> projections) {
        if (projections.isEmpty()) {
            return;
        }
        Set<String> outputFields = new java.util.LinkedHashSet<>();
        for (StaticModuleReadProjectionDefinition projection : projections) {
            if (!outputFields.add(projection.outputField())) {
                throw new IllegalArgumentException("duplicate static module read projection output field: "
                        + moduleAlias + "." + projection.outputField());
            }
        }
        if (entities.isEmpty()) {
            return;
        }
        Set<String> reservedFields = new java.util.LinkedHashSet<>();
        reservedFields.add(StandardEntitySchema.ID_FIELD);
        reservedFields.add(StandardEntitySchema.TENANT_ID_FIELD);
        reservedFields.add(StandardEntitySchema.VERSION_FIELD);
        reservedFields.add(StandardEntitySchema.DELETED_FIELD);
        reservedFields.add(StandardEntitySchema.DELETED_AT_FIELD);
        reservedFields.add(StandardEntitySchema.CREATED_BY_FIELD);
        reservedFields.add(StandardEntitySchema.CREATED_AT_FIELD);
        reservedFields.add(StandardEntitySchema.UPDATED_BY_FIELD);
        reservedFields.add(StandardEntitySchema.UPDATED_AT_FIELD);
        reservedFields.add(PlatformAbilityFields.TITLE_FIELD);
        reservedFields.add(PlatformAbilityFields.ENABLED_FIELD);
        reservedFields.add(PlatformAbilityFields.TREE_PARENT_FIELD);
        reservedFields.add(PlatformAbilityFields.SORT_FIELD);
        entities.getFirst().fields().stream()
                .map(net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition::fieldName)
                .forEach(reservedFields::add);
        for (StaticModuleReadProjectionDefinition projection : projections) {
            if (reservedFields.contains(projection.outputField())) {
                throw new IllegalArgumentException("static module read projection output field conflicts with main field: "
                        + moduleAlias + "." + projection.outputField());
            }
        }
    }

    private static void validateReferences(String moduleAlias, List<StaticModuleReferenceDefinition> references) {
        if (references.isEmpty()) {
            return;
        }
        Set<String> codes = new java.util.LinkedHashSet<>();
        for (StaticModuleReferenceDefinition reference : references) {
            if (!codes.add(reference.code())) {
                throw new IllegalArgumentException("duplicate static module reference code: "
                        + moduleAlias + "." + reference.code());
            }
        }
    }
}
