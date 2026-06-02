package net.ximatai.muyun.spring.ability.child;

import net.ximatai.muyun.spring.common.exception.PlatformException;
public record ChildPlan(
        String relationCode,
        String parentEntityAlias,
        String childEntityAlias,
        String childForeignKeyField,
        boolean autoPopulate,
        boolean autoDeleteWithParent
) {
    public ChildPlan {
        requireText(relationCode, "child relationCode");
        requireText(parentEntityAlias, "child parentEntityAlias");
        requireText(childEntityAlias, "child childEntityAlias");
        requireText(childForeignKeyField, "child childForeignKeyField");
        if (parentEntityAlias.equals(childEntityAlias)) {
            throw new PlatformException("child relation requires different parent and child entity: " + relationCode);
        }
    }

    public static ChildPlan of(String relationCode,
                               String parentEntityAlias,
                               String childEntityAlias,
                               String childForeignKeyField) {
        return new ChildPlan(relationCode, parentEntityAlias, childEntityAlias, childForeignKeyField, false, false);
    }

    public ChildPlan withAutoPopulate() {
        return new ChildPlan(relationCode, parentEntityAlias, childEntityAlias, childForeignKeyField, true, autoDeleteWithParent);
    }

    public ChildPlan withAutoDeleteWithParent() {
        return new ChildPlan(relationCode, parentEntityAlias, childEntityAlias, childForeignKeyField, autoPopulate, true);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(name + " must not be blank");
        }
    }
}
