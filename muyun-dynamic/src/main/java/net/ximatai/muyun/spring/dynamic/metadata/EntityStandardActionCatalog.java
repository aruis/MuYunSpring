package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.ArrayList;
import java.util.List;

public final class EntityStandardActionCatalog {
    private EntityStandardActionCatalog() {
    }

    public static List<EntityActionDefinition> from(EntityDefinition entity) {
        List<EntityActionDefinition> actions = new ArrayList<>();
        if (entity.supports(EntityCapability.CRUD)) {
            actions.add(action(entity, "create", EntityActionKind.RECORD, "Create", EntityActionLevel.LIST,
                    EntityActionStyle.PRIMARY));
            actions.add(action(entity, "select", EntityActionKind.RECORD, "Select", EntityActionLevel.RECORD,
                    EntityActionStyle.NORMAL));
            actions.add(action(entity, "update", EntityActionKind.RECORD, "Update"));
            actions.add(action(entity, "delete", EntityActionKind.RECORD, "Delete", EntityActionStyle.DANGER));
            actions.add(action(entity, "list", EntityActionKind.COLLECTION, "List"));
            actions.add(action(entity, "page", EntityActionKind.COLLECTION, "Page"));
            actions.add(action(entity, "count", EntityActionKind.QUERY, "Count"));
        }
        if (hasQueryableField(entity)) {
            actions.add(action(entity, "queryCriteria", EntityActionKind.QUERY, "Build query criteria"));
        }
        if (entity.supports(EntityCapability.SORT)) {
            actions.add(action(entity, "sortedList", EntityActionKind.SORT, "Sorted list"));
            actions.add(action(entity, "reorder", EntityActionKind.SORT, "Reorder"));
            actions.add(action(entity, "moveBefore", EntityActionKind.SORT, "Move before"));
            actions.add(action(entity, "moveAfter", EntityActionKind.SORT, "Move after"));
        }
        if (entity.supports(EntityCapability.TREE)) {
            actions.add(action(entity, "children", EntityActionKind.TREE, "Children"));
            actions.add(action(entity, "ancestorIds", EntityActionKind.TREE, "Ancestor ids"));
            actions.add(action(entity, "ancestorIdsAndSelf", EntityActionKind.TREE, "Ancestor ids and self"));
            actions.add(action(entity, "descendantIds", EntityActionKind.TREE, "Descendant ids"));
        }
        if (entity.supports(EntityCapability.REFERENCE)) {
            actions.add(action(entity, "title", EntityActionKind.REFERENCE, "Title"));
            actions.add(action(entity, "titles", EntityActionKind.REFERENCE, "Titles"));
            actions.add(action(entity, "projections", EntityActionKind.REFERENCE, "Projections"));
            actions.add(action(entity, "referenceOptions", EntityActionKind.REFERENCE, "Reference options"));
        }
        if (entity.supports(EntityCapability.ENABLE)) {
            actions.add(action(entity, "enable", EntityActionKind.STATE, "Enable"));
            actions.add(action(entity, "disable", EntityActionKind.STATE, "Disable"));
            actions.add(action(entity, "isEnabled", EntityActionKind.STATE, "Is enabled"));
            actions.add(action(entity, "enabledCriteria", EntityActionKind.STATE, "Enabled criteria"));
        }
        return List.copyOf(actions);
    }

    public static EntityActionKind standardKind(EntityDefinition entity, String actionCode) {
        return from(entity).stream()
                .filter(action -> action.actionCode().equals(actionCode))
                .map(EntityActionDefinition::kind)
                .findFirst()
                .orElse(null);
    }

    private static boolean hasQueryableField(EntityDefinition entity) {
        return entity.fields().stream().anyMatch(field -> field.queryDefinition().queryable());
    }

    private static EntityActionDefinition action(EntityDefinition entity,
                                                 String actionCode,
                                                 EntityActionKind kind,
                                                 String title) {
        return action(entity, actionCode, kind, title, EntityActionStyle.NORMAL);
    }

    private static EntityActionDefinition action(EntityDefinition entity,
                                                 String actionCode,
                                                 EntityActionKind kind,
                                                 String title,
                                                 EntityActionStyle style) {
        return action(entity, actionCode, kind, title, null, style);
    }

    private static EntityActionDefinition action(EntityDefinition entity,
                                                 String actionCode,
                                                 EntityActionKind kind,
                                                 String title,
                                                 EntityActionLevel actionLevel,
                                                 EntityActionStyle style) {
        return new EntityActionDefinition(entity.alias(), actionCode, kind, title, true, actionLevel, style,
                null, null, null, null, null, null, null, null, null);
    }
}
