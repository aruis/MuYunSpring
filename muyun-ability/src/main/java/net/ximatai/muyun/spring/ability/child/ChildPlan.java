package net.ximatai.muyun.spring.ability.child;

import net.ximatai.muyun.spring.common.exception.PlatformException;
public record ChildPlan(
        String relationCode,
        String parentEntity,
        String childEntity,
        String childForeignKeyField,
        boolean autoPopulate,
        boolean autoDeleteWithParent
) {
    public ChildPlan {
        requireText(relationCode, "child relationCode");
        requireText(parentEntity, "child parentEntity");
        requireText(childEntity, "child childEntity");
        requireText(childForeignKeyField, "child childForeignKeyField");
        if (parentEntity.equals(childEntity)) {
            throw new PlatformException("child relation requires different parent and child entity: " + relationCode);
        }
    }

    public static ChildPlan of(String relationCode,
                               String parentEntity,
                               String childEntity,
                               String childForeignKeyField) {
        return new ChildPlan(relationCode, parentEntity, childEntity, childForeignKeyField, false, false);
    }

    public ChildPlan withAutoPopulate() {
        return new ChildPlan(relationCode, parentEntity, childEntity, childForeignKeyField, true, autoDeleteWithParent);
    }

    public ChildPlan withAutoDeleteWithParent() {
        return new ChildPlan(relationCode, parentEntity, childEntity, childForeignKeyField, autoPopulate, true);
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(name + " must not be blank");
        }
    }
}
