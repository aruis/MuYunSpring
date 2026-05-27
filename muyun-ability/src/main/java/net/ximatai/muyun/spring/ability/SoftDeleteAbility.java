package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.model.EntityContract;
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
        afterSelect(entity);
        return entity;
    }

    default T selectIgnoreSoftDelete(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return getDao().query(CrudAbility.super.activeCriteria(Criteria.of().eq(StandardEntitySchema.ID_FIELD, id)), new PageRequest(0, 1))
                .stream()
                .findFirst()
                .orElse(null);
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
        return CrudAbility.super.update(entity);
    }

    @Override
    default int delete(String id) {
        beforeDelete(id);
        T entity = selectIgnoreSoftDelete(id);
        if (isSoftDeleted(entity)) {
            return 0;
        }
        EntityLifecycle.prepareDelete(entity, Instant.now());
        int deleted = getDao().updateById(entity);
        afterDelete(id, entity, deleted);
        if (deleted > 0) {
            afterChanged(entity);
            CacheInvalidationSupport.clearAfterChanged(this, entity);
        }
        return deleted;
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
