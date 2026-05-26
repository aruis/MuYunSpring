package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.id.Ids;
import net.ximatai.muyun.spring.common.model.BaseModel;
import net.ximatai.muyun.spring.common.model.TreeModel;

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
        validateTreePlacementIfNeeded(entity);
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
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        beforeUpdate(entity);
        validateTreePlacementIfNeeded(entity);
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
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1);
        return getDao().updateById(entity);
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

    default void afterSelect(T entity) {
    }

    default Criteria activeCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        scoped.andGroup(group -> group.eq("deleted", Boolean.FALSE).orIsNull("deleted"));
        return scoped;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void validateTreePlacementIfNeeded(T entity) {
        if (entity instanceof TreeModel && this instanceof TreeAbility treeAbility) {
            treeAbility.validateTreePlacement((TreeModel) entity);
        }
    }
}
