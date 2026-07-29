package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.child.ChildrenAbility;
import net.ximatai.muyun.spring.ability.deletion.DeletionContext;
import net.ximatai.muyun.spring.ability.deletion.DeletionLifecycleListener;
import net.ximatai.muyun.spring.ability.deletion.DeletionMode;
import net.ximatai.muyun.spring.ability.deletion.DeletionNode;
import net.ximatai.muyun.spring.ability.option.StaticOptionFieldValueValidator;
import net.ximatai.muyun.spring.ability.reference.ReferencerAbility;
import net.ximatai.muyun.spring.ability.reference.ReferenceDeletionGuard;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetResolver;
import net.ximatai.muyun.spring.ability.security.FieldProtectionAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

final class PlatformAbilityDispatcher {
    private static volatile StaticOptionFieldValueValidator staticOptionFieldValueValidator =
            StaticOptionFieldValueValidator.NONE;
    private static volatile DeletionLifecycleListener deletionLifecycleListener = DeletionLifecycleListener.NONE;
    private static volatile ReferenceDeletionGuard referenceDeletionGuard = ReferenceDeletionGuard.NONE;
    private static volatile ReferenceTargetResolver referenceTargetResolver = ReferenceTargetResolver.NONE;

    private PlatformAbilityDispatcher() {
    }

    static void setStaticOptionFieldValueValidator(StaticOptionFieldValueValidator validator) {
        staticOptionFieldValueValidator = validator == null ? StaticOptionFieldValueValidator.NONE : validator;
    }

    static void resetStaticOptionFieldValueValidator() {
        staticOptionFieldValueValidator = StaticOptionFieldValueValidator.NONE;
    }

    static void setDeletionLifecycleListener(DeletionLifecycleListener listener) {
        deletionLifecycleListener = listener == null ? DeletionLifecycleListener.NONE : listener;
    }

    static void resetDeletionLifecycleListener() {
        deletionLifecycleListener = DeletionLifecycleListener.NONE;
    }

    static void setReferenceDeletionGuard(ReferenceDeletionGuard guard) {
        referenceDeletionGuard = guard == null ? ReferenceDeletionGuard.NONE : guard;
    }

    static void resetReferenceDeletionGuard() {
        referenceDeletionGuard = ReferenceDeletionGuard.NONE;
    }

    static void setReferenceTargetResolver(ReferenceTargetResolver resolver) {
        referenceTargetResolver = resolver == null ? ReferenceTargetResolver.NONE : resolver;
    }

    static void resetReferenceTargetResolver() {
        referenceTargetResolver = ReferenceTargetResolver.NONE;
    }

    static ReferenceTargetResolver referenceTargetResolver() {
        return referenceTargetResolver;
    }

    static <T extends EntityContract> void beforeSoftDelete(CrudAbility<T> ability, T entity) {
        referenceDeletionGuard.beforeSoftDelete(ability, entity);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static <T extends EntityContract> void beforeRestore(CrudAbility<T> ability, T entity) {
        if (ability instanceof ReferencerAbility referencerAbility) {
            referencerAbility.validateReferenceIntegrity(entity);
        }
    }

    static DeletionContext rootDeletionContext(String moduleAlias, String recordId) {
        net.ximatai.muyun.spring.ability.deletion.DeletionResource root =
                new net.ximatai.muyun.spring.ability.deletion.DeletionResource(moduleAlias, recordId);
        return DeletionContext.root(moduleAlias, recordId, deletionLifecycleListener.open(root));
    }

    static DeletionContext resolveDeletionContext(String moduleAlias,
                                                  String recordId,
                                                  DeletionContext requestedContext) {
        if (requestedContext == null) {
            return rootDeletionContext(moduleAlias, recordId);
        }
        if (requestedContext.trigger() == net.ximatai.muyun.spring.ability.deletion.DeletionTrigger.DIRECT
                && !requestedContext.hasLifecycleSession()) {
            return rootDeletionContext(moduleAlias, recordId);
        }
        return requestedContext;
    }

    static <T extends EntityContract> DeletionNode deletionStarted(CrudAbility<T> ability,
                                                                    T entity,
                                                                    DeletionContext context,
                                                                    DeletionMode mode) {
        return context.lifecycleSession().started(ability, entity, context, mode);
    }

    static <T extends EntityContract> void deletionSucceeded(CrudAbility<T> ability,
                                                              T entity,
                                                              DeletionContext context,
                                                              DeletionNode node,
                                                              DeletionMode mode) {
        context.lifecycleSession().succeeded(ability, entity, context, node, mode);
    }

    static <T extends EntityContract> void deletionFailed(CrudAbility<T> ability,
                                                           T entity,
                                                           DeletionContext context,
                                                           DeletionNode node,
                                                           DeletionMode mode,
                                                           RuntimeException failure) {
        context.lifecycleSession().failed(ability, entity, context, node, mode, failure);
    }

    static <T extends EntityContract> void beforeSave(CrudAbility<T> ability, T entity) {
        runStaticOptionFieldValidation(ability, entity);
        runReferenceIntegrityValidation(ability, entity);
        TenantUniqueConstraintSupport.validate(ability, entity);
    }

    static <T extends EntityContract> void afterInsert(CrudAbility<T> ability, String id, T entity) {
        runChildrenAfterInsert(ability, id, entity);
        ability.afterPlatformInsert(id, entity);
    }

    static <T extends EntityContract> void afterUpdate(CrudAbility<T> ability, T entity, int updated) {
        runChildrenAfterUpdate(ability, entity, updated);
        ability.afterPlatformUpdate(entity, updated);
    }

    static <T extends EntityContract> void afterDelete(CrudAbility<T> ability,
                                                        String id,
                                                        T entity,
                                                        int deleted,
                                                        DeletionContext context,
                                                        DeletionNode node) {
        runChildrenAfterDelete(ability, id, entity, deleted, context, node);
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

    private static <T extends EntityContract> void runStaticOptionFieldValidation(CrudAbility<T> ability, T entity) {
        if (ability == null || entity == null) {
            return;
        }
        Class<?> modelClass = ability.modelClass() == null ? entity.getClass() : ability.modelClass();
        staticOptionFieldValueValidator.validate(modelClass, entity);
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
    private static <T extends EntityContract> void runChildrenAfterDelete(CrudAbility<T> ability,
                                                                           String id,
                                                                           T entity,
                                                                           int deleted,
                                                                           DeletionContext context,
                                                                           DeletionNode node) {
        if (ability instanceof ChildrenAbility childrenAbility) {
            childrenAbility.afterChildrenDelete(id, entity, deleted, context, node);
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
    private static <T extends EntityContract> void runReferenceIntegrityValidation(CrudAbility<T> ability, T entity) {
        if (ability instanceof ReferencerAbility referencerAbility) {
            referencerAbility.validateReferenceIntegrity(entity);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T extends EntityContract> void runFieldProtectionAfterSelect(CrudAbility<T> ability, T entity) {
        if (ability instanceof FieldProtectionAbility fieldProtectionAbility) {
            fieldProtectionAbility.restoreProtectedFieldsFromStorage(entity);
        }
    }
}
