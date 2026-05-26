package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import net.ximatai.muyun.spring.common.model.EntityContract;
import net.ximatai.muyun.spring.common.model.EnabledCapable;
import net.ximatai.muyun.spring.common.model.TreeCapable;

import java.time.Instant;

public interface CrudAbility<T extends EntityContract> {
    BaseDao<T, String> getDao();

    String getModuleAlias();

    default String insert(T entity) {
        EntityLifecycle.prepareInsert(entity, Instant.now());
        prepareAbilityDefaults(entity);
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
        EntityLifecycle.prepareUpdate(entity, Instant.now(), nextVersionForUpdate(entity));
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
        EntityLifecycle.prepareDelete(entity, Instant.now());
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

    default Integer nextVersionForUpdate(T entity) {
        return EntityLifecycle.nextVersion(entity.getVersion());
    }

    default Criteria activeCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        scoped.andGroup(group -> group.eq("deleted", Boolean.FALSE).orIsNull("deleted"));
        return scoped;
    }

    private void prepareAbilityDefaults(T entity) {
        if (entity instanceof TreeCapable tree
                && this instanceof TreeAbility<?>
                && (tree.getParentId() == null || tree.getParentId().isBlank())) {
            tree.setParentId(TreeAbility.ROOT_ID);
        }
        if (entity instanceof EnabledCapable enabled && enabled.getEnabled() == null) {
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
