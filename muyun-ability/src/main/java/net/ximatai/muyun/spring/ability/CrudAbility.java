package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.id.Ids;
import net.ximatai.muyun.spring.common.model.BaseModel;

import java.time.Instant;

public interface CrudAbility<T extends BaseModel> {
    BaseDao<T, String> getDao();

    String getModuleAlias();

    default String insert(T entity) {
        if (entity.getId() == null || entity.getId().isBlank()) {
            entity.setId(Ids.newId());
        }
        Instant now = Instant.now();
        entity.setVersion(entity.getVersion() == null ? 0 : entity.getVersion());
        entity.setDeleted(Boolean.FALSE);
        entity.setCreatedAt(entity.getCreatedAt() == null ? now : entity.getCreatedAt());
        entity.setUpdatedAt(now);
        beforeInsert(entity);
        return getDao().insert(entity);
    }

    default T select(String id) {
        T entity = getDao().findById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            return null;
        }
        afterSelect(entity);
        return entity;
    }

    default int update(T entity) {
        entity.setUpdatedAt(Instant.now());
        beforeUpdate(entity);
        return getDao().updateById(entity);
    }

    default int delete(String id) {
        beforeDelete(id);
        T entity = getDao().findById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            return 0;
        }
        entity.setDeleted(Boolean.TRUE);
        entity.setUpdatedAt(Instant.now());
        return getDao().updateById(entity);
    }

    default PageResult<T> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        Criteria scoped = criteria == null ? Criteria.of() : criteria;
        scoped.eq("deleted", Boolean.FALSE);
        return getDao().pageQuery(scoped, pageRequest, sorts);
    }

    default void beforeInsert(T entity) {
    }

    default void beforeUpdate(T entity) {
    }

    default void beforeDelete(String id) {
    }

    default void afterSelect(T entity) {
    }
}
