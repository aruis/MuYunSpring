package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

public interface EnableAbility<T extends EnabledCapable> extends CrudAbility<T> {
    default int enable(String id) {
        return updateEnabled(id, Boolean.TRUE);
    }

    default int disable(String id) {
        return updateEnabled(id, Boolean.FALSE);
    }

    default boolean isEnabled(String id) {
        T entity = selectActiveRaw(id);
        return entity != null && Boolean.TRUE.equals(entity.getEnabled());
    }

    default T requireEnabled(String id, String message) {
        T entity = selectActiveRaw(id);
        if (entity == null || !Boolean.TRUE.equals(entity.getEnabled())) {
            throw new PlatformException(message);
        }
        return entity;
    }

    default Criteria enabledCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        scoped.eq(PlatformAbilityFields.ENABLED_FIELD, Boolean.TRUE);
        return scoped;
    }

    private int updateEnabled(String id, Boolean enabled) {
        T entity = selectActiveRaw(id);
        if (entity == null) {
            return 0;
        }
        entity.setEnabled(enabled);
        return update(entity);
    }
}
