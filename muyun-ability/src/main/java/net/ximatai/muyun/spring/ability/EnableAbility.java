package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

import java.util.Objects;
import java.util.function.Supplier;

public interface EnableAbility<T extends EnabledCapable> extends CrudAbility<T> {
    default int enable(String id) {
        return updateEnabled(id, Boolean.TRUE);
    }

    @PlatformOperation(PlatformAction.ENABLE)
    default int enable(String id, Integer expectedVersion) {
        return updateEnabled(id, Boolean.TRUE, expectedVersion);
    }

    default int disable(String id) {
        return updateEnabled(id, Boolean.FALSE);
    }

    @PlatformOperation(PlatformAction.DISABLE)
    default int disable(String id, Integer expectedVersion) {
        return updateEnabled(id, Boolean.FALSE, expectedVersion);
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

    /**
     * Requires an enabled record while allowing the calling business service to
     * preserve its own error contract.
     */
    default T requireEnabledOrThrow(String id, Supplier<? extends RuntimeException> exceptionSupplier) {
        T entity = selectActiveRaw(id);
        if (entity == null || !Boolean.TRUE.equals(entity.getEnabled())) {
            throw Objects.requireNonNull(exceptionSupplier, "exceptionSupplier must not be null").get();
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
        return updateEnabled(id, enabled, null);
    }

    private int updateEnabled(String id, Boolean enabled, Integer expectedVersion) {
        T entity = selectActiveRaw(id);
        if (entity == null) {
            return 0;
        }
        entity.setEnabled(enabled);
        if (expectedVersion != null) {
            entity.setVersion(expectedVersion);
        }
        return update(entity);
    }
}
