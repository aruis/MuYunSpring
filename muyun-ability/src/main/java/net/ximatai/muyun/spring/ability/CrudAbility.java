package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public interface CrudAbility<T extends EntityContract> {
    BaseDao<T, String> getDao();

    String getModuleAlias();

    default Class<?> modelClass() {
        return null;
    }

    default String insert(T entity) {
        beforePrepareInsert(entity);
        EntityLifecycle.prepareInsert(entity, Instant.now());
        prepareAbilityDefaults(entity);
        beforeInsert(entity);
        prepareSortDefault(entity);
        validateTreePlacementIfNeeded(entity);
        String id = getDao().insert(entity);
        PlatformAbilityDispatcher.afterInsert(this, id, entity);
        afterInsert(id, entity);
        afterChanged(entity);
        CacheInvalidationSupport.clearAfterChanged(this, entity);
        return id;
    }

    default List<String> insertBatch(Collection<T> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (T entity : entities) {
            ids.add(insert(entity));
        }
        return ids;
    }

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

    default int update(T entity) {
        DataScopeCriteriaResult mutationScope = mutationRecordScope(PlatformAction.UPDATE, entity == null ? null : entity.getId());
        return withTenantScope(mutationScope, () -> {
            T existing = selectExistingForScopedMutation(entity);
            if (TenantContext.currentTenantId().isPresent() && existing == null) {
                return 0;
            }
            if (existing != null) {
                entity.setTenantId(existing.getTenantId());
            }
            Integer expectedVersion = expectedVersionForUpdate(entity);
            EntityLifecycle.prepareUpdate(entity, Instant.now(), EntityLifecycle.nextVersion(expectedVersion));
            beforeUpdate(entity);
            validateTreePlacementIfNeeded(entity);
            int updated = getDao().updateByIdAndVersion(entity, expectedVersion);
            if (updated <= 0) {
                throw new OptimisticLockException("record version conflict: " + entity.getId());
            }
            PlatformAbilityDispatcher.afterUpdate(this, entity, updated);
            afterUpdate(entity, updated);
            afterChanged(entity);
            CacheInvalidationSupport.clearAfterChanged(this, entity);
            return updated;
        });
    }

    default int delete(String id) {
        return delete(id, null);
    }

    default int delete(T entity) {
        if (entity == null || entity.getId() == null || entity.getId().isBlank()) {
            return 0;
        }
        return delete(entity.getId(), entity.getVersion());
    }

    default int delete(String id, Integer expectedVersion) {
        beforeDelete(id);
        DataScopeCriteriaResult mutationScope = mutationRecordScope(PlatformAction.DELETE, id);
        return withTenantScope(mutationScope, () -> {
            T entity = selectActiveRaw(id);
            if (entity == null) {
                return 0;
            }
            Integer effectiveExpectedVersion = expectedVersion == null ? entity.getVersion() : expectedVersion;
            int deleted = getDao().deleteByIdAndVersion(id, effectiveExpectedVersion);
            if (deleted <= 0) {
                throw new OptimisticLockException("record version conflict: " + id);
            }
            PlatformAbilityDispatcher.afterDelete(this, id, entity, deleted);
            afterDelete(id, entity, deleted);
            afterChanged(entity);
            CacheInvalidationSupport.clearAfterChanged(this, entity);
            return deleted;
        });
    }

    default int deleteBatch(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        DataScopeCriteriaResult mutationScope = mutationRecordScope(PlatformAction.DELETE, ids);
        return withTenantScope(mutationScope, () -> {
            int count = 0;
            for (String id : ids) {
                count += delete(id);
            }
            return count;
        });
    }

    default PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return getDao().pageQuery(activeCriteria(criteria), pageRequest, sorts);
    }

    default List<T> list(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return getDao().query(activeCriteria(criteria), pageRequest, sorts);
    }

    default long count(Criteria criteria) {
        return getDao().count(activeCriteria(criteria));
    }

    default void beforeInsert(T entity) {
    }

    /**
     * Runs before the platform fills insert lifecycle fields. Use only for checks or normalization
     * that must happen before an empty id would be auto-generated.
     */
    default void beforePrepareInsert(T entity) {
    }

    default void beforeUpdate(T entity) {
    }

    default void beforeDelete(String id) {
    }

    default void afterPlatformInsert(String id, T entity) {
    }

    default void afterPlatformUpdate(T entity, int updated) {
    }

    default void afterPlatformDelete(String id, T entity, int deleted) {
    }

    default void afterPlatformSelect(T entity) {
    }

    default void afterInsert(String id, T entity) {
    }

    default void afterUpdate(T entity, int updated) {
    }

    default void afterDelete(String id, T entity, int deleted) {
    }

    default void afterChanged(T entity) {
    }

    default void afterSelect(T entity) {
    }

    default Integer nextVersionForUpdate(T entity) {
        return EntityLifecycle.nextVersion(entity.getVersion());
    }

    default Integer expectedVersionForUpdate(T entity) {
        if (entity.getVersion() != null) {
            return entity.getVersion();
        }
        T current = selectActiveRaw(entity.getId());
        if (current == null) {
            throw new IllegalArgumentException("record not found: " + entity.getId());
        }
        return current.getVersion();
    }

    default boolean shouldPrepareTreeDefault(T entity) {
        return true;
    }

    default boolean shouldPrepareEnabledDefault(T entity) {
        return this instanceof EnableAbility<?>;
    }

    default Criteria activeCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        if (!TenantContext.tenantFilterBypassed()) {
            TenantContext.currentTenantId()
                    .ifPresent(tenantId -> scoped.eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId));
        }
        return scoped;
    }

    default T selectActiveRaw(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return getDao().query(activeCriteria(Criteria.of().eq(StandardEntitySchema.ID_FIELD, id)), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    private T selectExistingForScopedMutation(T entity) {
        if (entity == null || entity.getId() == null || entity.getId().isBlank()) {
            return null;
        }
        return TenantContext.currentTenantId().isPresent() || TenantContext.isSystem()
                ? selectActiveRaw(entity.getId())
                : null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DataScopeCriteriaResult mutationRecordScope(PlatformAction action, String id) {
        if (id == null || id.isBlank()) {
            return DataScopeCriteriaResult.unrestricted(Criteria.of());
        }
        if (this instanceof DataScopeAbility dataScopeAbility) {
            return dataScopeAbility.requireRecordScopeResult(mutationPolicy(action), List.of(id));
        }
        return DataScopeCriteriaResult.unrestricted(Criteria.of());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private DataScopeCriteriaResult mutationRecordScope(PlatformAction action, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return DataScopeCriteriaResult.unrestricted(Criteria.of());
        }
        if (this instanceof DataScopeAbility dataScopeAbility) {
            return dataScopeAbility.requireRecordScopeResult(mutationPolicy(action), ids);
        }
        return DataScopeCriteriaResult.unrestricted(Criteria.of());
    }

    private ActionExecutionPolicy mutationPolicy(PlatformAction fallback) {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(getModuleAlias()))
                .map(ActionExecutionContext::actionPolicy)
                .orElseGet(fallback::executionPolicy);
    }

    private <R> R withTenantScope(DataScopeCriteriaResult scope, Supplier<R> supplier) {
        if (scope != null && scope.crossTenant()) {
            try (TenantContext.Scope ignored = TenantContext.bypassTenantFilter("data scope allows cross-tenant mutation")) {
                return supplier.get();
            }
        }
        return supplier.get();
    }

    private void prepareAbilityDefaults(T entity) {
        if (entity instanceof TreeCapable tree
                && this instanceof TreeAbility<?>
                && shouldPrepareTreeDefault(entity)
                && (tree.getParentId() == null || tree.getParentId().isBlank())) {
            tree.setParentId(TreeAbility.ROOT_ID);
        }
        if (entity instanceof EnabledCapable enabled
                && shouldPrepareEnabledDefault(entity)
                && enabled.getEnabled() == null) {
            enabled.setEnabled(Boolean.TRUE);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void prepareSortDefault(T entity) {
        if (entity instanceof SortCapable && this instanceof SortAbility sortAbility) {
            sortAbility.prepareSortOrderForInsert((SortCapable) entity);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void validateTreePlacementIfNeeded(T entity) {
        if (entity instanceof TreeCapable && this instanceof TreeAbility treeAbility) {
            treeAbility.validateTreePlacement((TreeCapable) entity);
        }
    }

}
