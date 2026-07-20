package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;

import java.util.List;
import java.util.Set;

public final class StaticModuleDefinitionTestFactory {
    private StaticModuleDefinitionTestFactory() {
    }

    public static StaticModuleDefinition create(String applicationAlias,
                                                String moduleAlias,
                                                String title,
                                                String parentModuleAlias,
                                                List<StaticModuleActionDefinition> actions) {
        return base(applicationAlias, moduleAlias, title, parentModuleAlias)
                .actions(actions)
                .build();
    }

    public static StaticModuleDefinition create(String applicationAlias,
                                                String moduleAlias,
                                                String title,
                                                String parentModuleAlias,
                                                Set<EntityCapability> capabilities,
                                                List<StaticModuleActionDefinition> actions) {
        return base(applicationAlias, moduleAlias, title, parentModuleAlias)
                .capabilities(capabilities)
                .actions(actions)
                .build();
    }

    public static StaticModuleDefinition create(String applicationAlias,
                                                String moduleAlias,
                                                String title,
                                                String parentModuleAlias,
                                                ModuleEntryType entryType,
                                                String entryRoute,
                                                String entryExternalUrl,
                                                Set<EntityCapability> capabilities,
                                                List<StaticModuleActionDefinition> actions,
                                                List<EntityDefinition> entities) {
        return base(applicationAlias, moduleAlias, title, parentModuleAlias)
                .entry(entryType, entryRoute, entryExternalUrl)
                .capabilities(capabilities)
                .actions(actions)
                .entities(entities)
                .build();
    }

    public static StaticModuleDefinition create(String applicationAlias,
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
        return base(applicationAlias, moduleAlias, title, parentModuleAlias)
                .entry(entryType, entryRoute, entryExternalUrl)
                .capabilities(capabilities)
                .actions(actions)
                .entities(entities)
                .uiDefinition(uiDefinition)
                .build();
    }

    public static StaticModuleDefinition create(String applicationAlias,
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
        return base(applicationAlias, moduleAlias, title, parentModuleAlias)
                .entry(entryType, entryRoute, entryExternalUrl)
                .capabilities(capabilities)
                .actions(actions)
                .entities(entities)
                .uiDefinition(uiDefinition)
                .projectionJoins(projectionJoins)
                .build();
    }

    public static StaticModuleDefinition create(String applicationAlias,
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
        return base(applicationAlias, moduleAlias, title, parentModuleAlias)
                .entry(entryType, entryRoute, entryExternalUrl)
                .capabilities(capabilities)
                .actions(actions)
                .entities(entities)
                .uiDefinition(uiDefinition)
                .references(references)
                .readProjections(readProjections)
                .build();
    }

    public static StaticModuleDefinition create(String applicationAlias,
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
        return base(applicationAlias, moduleAlias, title, parentModuleAlias)
                .entry(entryType, entryRoute, entryExternalUrl)
                .capabilities(capabilities)
                .actions(actions)
                .entities(entities)
                .uiDefinition(uiDefinition)
                .references(references)
                .readProjections(readProjections)
                .projectionJoins(projectionJoins)
                .build();
    }

    public static StaticModuleDefinition create(String applicationAlias,
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
                                                List<RelationProjectionJoinDefinition> projectionJoins) {
        return new StaticModuleDefinition(applicationAlias, moduleAlias, title, parentModuleAlias, entryType,
                entryRoute, entryExternalUrl, capabilities, actions, entities, uiDefinition, references,
                readProjections, modelClass, projectionJoins);
    }

    private static StaticModuleDefinition.Builder base(String applicationAlias,
                                                       String moduleAlias,
                                                       String title,
                                                       String parentModuleAlias) {
        return StaticModuleDefinition.builder(applicationAlias, moduleAlias, title)
                .parentModuleAlias(parentModuleAlias);
    }
}
