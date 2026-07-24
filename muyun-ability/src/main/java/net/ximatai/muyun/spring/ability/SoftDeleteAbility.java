package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.security.FieldProtectionAbility;
import net.ximatai.muyun.spring.ability.deletion.DeletionContext;
import net.ximatai.muyun.spring.ability.deletion.DeletionMode;
import net.ximatai.muyun.spring.ability.deletion.DeletionNode;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;

import java.time.Instant;

public interface SoftDeleteAbility<T extends EntityContract> extends CrudAbility<T> {
    @Override
    default T select(String id) {
        if (this instanceof CacheAbility<?> cacheAbility) {
            @SuppressWarnings("unchecked")
            T cached = (T) cacheAbility.selectWithCache(id);
            return cached;
        }
        T entity = selectActiveRaw(id);
        if (entity == null) {
            return null;
        }
        PlatformAbilityDispatcher.afterSelect(this, entity);
        afterSelect(entity);
        return entity;
    }

    default T selectIgnoreSoftDelete(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        T entity = getDao().query(CrudAbility.super.activeCriteria(Criteria.of().eq(StandardEntitySchema.ID_FIELD, id)), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
        if (entity != null && this instanceof FieldProtectionAbility<?> fieldProtectionAbility) {
            @SuppressWarnings("unchecked")
            FieldProtectionAbility<T> typed = (FieldProtectionAbility<T>) fieldProtectionAbility;
            typed.restoreProtectedFieldsFromStorage(entity);
        }
        return entity;
    }

    @Override
    default int update(T entity) {
        if (entity == null || entity.getId() == null || entity.getId().isBlank()) {
            return 0;
        }
        T active = selectActiveRaw(entity.getId());
        if (active == null) {
            return 0;
        }
        entity.setTenantId(active.getTenantId());
        entity.setDeleted(Boolean.FALSE);
        entity.setDeletedAt(null);
        entity.setDeletedBy(null);
        return CrudAbility.super.update(entity);
    }

    @Override
    default int delete(String id) {
        return delete(id, null);
    }

    @Override
    default int delete(T entity) {
        if (entity == null || entity.getId() == null || entity.getId().isBlank()) {
            return 0;
        }
        return delete(entity.getId(), entity.getVersion());
    }

    @Override
    default int delete(String id, Integer expectedVersion) {
        if (id == null || id.isBlank()) {
            return 0;
        }
        return delete(id, expectedVersion, DeletionContext.root(getModuleAlias(), id));
    }

    @Override
    default int delete(String id, Integer expectedVersion, DeletionContext deletionContext) {
        if (id == null || id.isBlank()) {
            return 0;
        }
        DeletionContext context = deletionContext == null
                ? DeletionContext.root(getModuleAlias(), id)
                : deletionContext;
        beforeDelete(id, context);
        T entity = selectIgnoreSoftDelete(id);
        if (isSoftDeleted(entity)) {
            return 0;
        }
        PlatformManagedMutationGuard.beforeDelete(this, entity);
        if (expectedVersion != null && !expectedVersion.equals(entity.getVersion())) {
            throw new OptimisticLockException("record version conflict: " + id);
        }
        Integer effectiveExpectedVersion = expectedVersion == null ? entity.getVersion() : expectedVersion;
        DeletionNode node = PlatformAbilityDispatcher.deletionStarted(this, entity, context, DeletionMode.SOFT);
        try {
            EntityLifecycle.prepareDelete(entity, Instant.now());
            int deleted;
            try (FieldProtectionAbility.FieldProtectionMutation ignored = PlatformAbilityDispatcher.beforePersist(this, entity)) {
                deleted = getDao().updateByIdAndVersion(entity, effectiveExpectedVersion);
            }
            if (deleted <= 0) {
                throw new OptimisticLockException("record version conflict: " + id);
            }
            PlatformAbilityDispatcher.afterDelete(this, id, entity, deleted, context, node);
            afterDelete(id, entity, deleted);
            afterChanged(entity);
            CacheInvalidationSupport.clearAfterChanged(this, entity);
            PlatformAbilityDispatcher.deletionSucceeded(this, entity, context, node, DeletionMode.SOFT);
            return deleted;
        } catch (RuntimeException exception) {
            PlatformAbilityDispatcher.deletionFailed(this, entity, context, node, DeletionMode.SOFT, exception);
            throw exception;
        }
    }

    @Override
    default Criteria activeCriteria(Criteria criteria) {
        Criteria scoped = CrudAbility.super.activeCriteria(criteria);
        scoped.andGroup(group -> group
                .eq(StandardEntitySchema.DELETED_FIELD, Boolean.FALSE)
                .orIsNull(StandardEntitySchema.DELETED_FIELD));
        return scoped;
    }

    private boolean isSoftDeleted(T entity) {
        return entity == null || Boolean.TRUE.equals(entity.getDeleted());
    }

}
