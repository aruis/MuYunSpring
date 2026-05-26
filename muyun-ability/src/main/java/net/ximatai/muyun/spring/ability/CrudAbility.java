package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.model.EntityContract;
import net.ximatai.muyun.spring.common.model.EnabledCapable;
import net.ximatai.muyun.spring.common.model.TreeCapable;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public interface CrudAbility<T extends EntityContract> {
    BaseDao<T, String> getDao();

    String getModuleAlias();

    default String insert(T entity) {
        EntityLifecycle.prepareInsert(entity, Instant.now());
        prepareAbilityDefaults(entity);
        beforeInsert(entity);
        validateTreePlacementIfNeeded(entity);
        String id = getDao().insert(entity);
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
        afterSelect(entity);
        return entity;
    }

    default int update(T entity) {
        T existing = selectExistingForScopedMutation(entity);
        if (TenantContext.currentTenantId().isPresent() && existing == null) {
            return 0;
        }
        if (existing != null) {
            entity.setTenantId(existing.getTenantId());
        }
        EntityLifecycle.prepareUpdate(entity, Instant.now(), nextVersionForUpdate(entity));
        beforeUpdate(entity);
        validateTreePlacementIfNeeded(entity);
        int updated = getDao().updateById(entity);
        afterUpdate(entity, updated);
        if (updated > 0) {
            afterChanged(entity);
            CacheInvalidationSupport.clearAfterChanged(this, entity);
        }
        return updated;
    }

    default int delete(String id) {
        beforeDelete(id);
        T entity = selectActiveRaw(id);
        if (entity == null) {
            return 0;
        }
        int deleted = getDao().deleteById(id);
        afterDelete(id, entity, deleted);
        if (deleted > 0) {
            afterChanged(entity);
            CacheInvalidationSupport.clearAfterChanged(this, entity);
        }
        return deleted;
    }

    default int delete(T entity) {
        if (entity == null || entity.getId() == null || entity.getId().isBlank()) {
            return 0;
        }
        return delete(entity.getId());
    }

    default int deleteBatch(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String id : ids) {
            count += delete(id);
        }
        return count;
    }

    default PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return getDao().pageQuery(activeCriteria(criteria), pageRequest, sorts);
    }

    default long count(Criteria criteria) {
        return getDao().count(activeCriteria(criteria));
    }

    default void beforeInsert(T entity) {
    }

    default void beforeUpdate(T entity) {
    }

    default void beforeDelete(String id) {
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

    default boolean shouldPrepareTreeDefault(T entity) {
        return true;
    }

    default boolean shouldPrepareEnabledDefault(T entity) {
        return true;
    }

    default Criteria activeCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        TenantContext.currentTenantId()
                .ifPresent(tenantId -> scoped.eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId));
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
        return TenantContext.currentTenantId().isPresent() ? selectActiveRaw(entity.getId()) : null;
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
    private void validateTreePlacementIfNeeded(T entity) {
        if (entity instanceof TreeCapable && this instanceof TreeAbility treeAbility) {
            treeAbility.validateTreePlacement((TreeCapable) entity);
        }
    }

}
