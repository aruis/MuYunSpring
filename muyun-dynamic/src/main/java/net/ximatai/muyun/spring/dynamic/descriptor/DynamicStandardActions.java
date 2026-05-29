package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;

import java.util.ArrayList;
import java.util.List;

final class DynamicStandardActions {
    private DynamicStandardActions() {
    }

    static List<DynamicActionDescriptor> from(EntityDefinition entity) {
        List<DynamicActionDescriptor> actions = new ArrayList<>();
        if (entity.supports(EntityCapability.CRUD)) {
            actions.add(action("create", DynamicActionKind.RECORD, "Create"));
            actions.add(action("select", DynamicActionKind.RECORD, "Select"));
            actions.add(action("update", DynamicActionKind.RECORD, "Update"));
            actions.add(action("delete", DynamicActionKind.RECORD, "Delete"));
            actions.add(action("list", DynamicActionKind.COLLECTION, "List"));
            actions.add(action("page", DynamicActionKind.COLLECTION, "Page"));
            actions.add(action("count", DynamicActionKind.QUERY, "Count"));
        }
        if (hasQueryableField(entity)) {
            actions.add(action("queryCriteria", DynamicActionKind.QUERY, "Build query criteria"));
        }
        if (entity.supports(EntityCapability.SORT)) {
            actions.add(action("sortedList", DynamicActionKind.SORT, "Sorted list"));
            actions.add(action("reorder", DynamicActionKind.SORT, "Reorder"));
            actions.add(action("moveBefore", DynamicActionKind.SORT, "Move before"));
            actions.add(action("moveAfter", DynamicActionKind.SORT, "Move after"));
        }
        if (entity.supports(EntityCapability.TREE)) {
            actions.add(action("children", DynamicActionKind.TREE, "Children"));
            actions.add(action("ancestorIds", DynamicActionKind.TREE, "Ancestor ids"));
            actions.add(action("ancestorIdsAndSelf", DynamicActionKind.TREE, "Ancestor ids and self"));
            actions.add(action("descendantIds", DynamicActionKind.TREE, "Descendant ids"));
        }
        if (entity.supports(EntityCapability.REFERENCE)) {
            actions.add(action("title", DynamicActionKind.REFERENCE, "Title"));
            actions.add(action("titles", DynamicActionKind.REFERENCE, "Titles"));
            actions.add(action("projections", DynamicActionKind.REFERENCE, "Projections"));
            actions.add(action("referenceOptions", DynamicActionKind.REFERENCE, "Reference options"));
        }
        if (entity.supports(EntityCapability.ENABLE)) {
            actions.add(action("enable", DynamicActionKind.STATE, "Enable"));
            actions.add(action("disable", DynamicActionKind.STATE, "Disable"));
            actions.add(action("isEnabled", DynamicActionKind.STATE, "Is enabled"));
            actions.add(action("enabledCriteria", DynamicActionKind.STATE, "Enabled criteria"));
        }
        return List.copyOf(actions);
    }

    private static boolean hasQueryableField(EntityDefinition entity) {
        return entity.fields().stream().anyMatch(field -> field.queryDefinition().queryable());
    }

    private static DynamicActionDescriptor action(String code, DynamicActionKind kind, String title) {
        return new DynamicActionDescriptor(code, kind, title);
    }
}
