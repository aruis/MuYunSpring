package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.ability.security.FieldProtectionAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

final class PlatformAbilityDispatcher {
    private PlatformAbilityDispatcher() {
    }

    static <T extends EntityContract> void afterInsert(CrudAbility<T> ability, String id, T entity) {
        runChildrenAfterInsert(ability, id, entity);
        ability.afterPlatformInsert(id, entity);
    }

    static <T extends EntityContract> void afterUpdate(CrudAbility<T> ability, T entity, int updated) {
        runChildrenAfterUpdate(ability, entity, updated);
        ability.afterPlatformUpdate(entity, updated);
    }

    static <T extends EntityContract> void afterDelete(CrudAbility<T> ability, String id, T entity, int deleted) {
        runChildrenAfterDelete(ability, id, entity, deleted);
        ability.afterPlatformDelete(id, entity, deleted);
    }

    static <T extends EntityContract> void afterSelect(CrudAbility<T> ability, T entity) {
        runFieldProtectionAfterSelect(ability, entity);
        runChildrenAfterSelect(ability, entity);
        runReferenceAfterSelect(ability, entity);
        ability.afterPlatformSelect(entity);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static <T extends EntityContract> FieldProtectionAbility.FieldProtectionMutation beforePersist(CrudAbility<T> ability,
                                                                                                   T entity) {
        if (ability instanceof FieldProtectionAbility fieldProtectionAbility) {
            return fieldProtectionAbility.protectFieldsForStorage(entity);
        }
        return FieldProtectionAbility.FieldProtectionMutation.NONE;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> void runChildrenAfterInsert(CrudAbility<T> ability, String id, T entity) {
        if (ability instanceof ChildrenAbility childrenAbility) {
            childrenAbility.afterChildrenInsert(id, entity);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> void runChildrenAfterUpdate(CrudAbility<T> ability, T entity, int updated) {
        if (ability instanceof ChildrenAbility childrenAbility) {
            childrenAbility.afterChildrenUpdate(entity, updated);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> void runChildrenAfterDelete(CrudAbility<T> ability, String id, T entity, int deleted) {
        if (ability instanceof ChildrenAbility childrenAbility) {
            childrenAbility.afterChildrenDelete(id, entity, deleted);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> void runChildrenAfterSelect(CrudAbility<T> ability, T entity) {
        if (ability instanceof ChildrenAbility childrenAbility) {
            childrenAbility.afterChildrenSelect(entity);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> void runReferenceAfterSelect(CrudAbility<T> ability, T entity) {
        if (ability instanceof ReferencerAbility referencerAbility) {
            referencerAbility.afterReferenceSelect(entity);
            referencerAbility.refreshReferenceDependencies(entity);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> void runFieldProtectionAfterSelect(CrudAbility<T> ability, T entity) {
        if (ability instanceof FieldProtectionAbility fieldProtectionAbility) {
            fieldProtectionAbility.restoreProtectedFieldsFromStorage(entity);
        }
    }
}
