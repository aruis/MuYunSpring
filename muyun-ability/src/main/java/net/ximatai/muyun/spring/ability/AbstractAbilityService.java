package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.util.Preconditions;

import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractAbilityService<T extends EntityContract> implements CrudAbility<T> {
    private final String moduleAlias;
    private final Class<T> modelClass;
    private final BaseDao<T, String> dao;

    protected AbstractAbilityService(String moduleAlias, Class<T> modelClass, BaseDao<T, String> dao) {
        this.moduleAlias = Preconditions.requireText(moduleAlias, "moduleAlias");
        this.modelClass = Objects.requireNonNull(modelClass, "modelClass must not be null");
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
    }

    @Override
    public final BaseDao<T, String> getDao() {
        return dao;
    }

    @Override
    public final String getModuleAlias() {
        return moduleAlias;
    }

    @Override
    public final Class<T> modelClass() {
        return modelClass;
    }

    protected final boolean existsOtherInCurrentScope(T entity, Criteria criteria) {
        String currentId = entity == null ? null : entity.getId();
        return list(criteria, PageRequest.of(1, 2)).stream()
                .anyMatch(existing -> !Objects.equals(existing.getId(), currentId));
    }

    protected final T findOne(Criteria criteria) {
        return list(criteria, PageRequest.of(1, 1))
                .stream()
                .findFirst()
                .orElse(null);
    }

    protected final T requireOne(Criteria criteria, String message) {
        T existing = findOne(criteria);
        if (existing == null) {
            throw new PlatformException(message);
        }
        return existing;
    }

    protected final T selectIncludingDeleted(String id) {
        if (this instanceof SoftDeleteAbility<?> softDeleteAbility) {
            @SuppressWarnings("unchecked")
            SoftDeleteAbility<T> typed = (SoftDeleteAbility<T>) softDeleteAbility;
            return typed.selectIgnoreSoftDelete(id);
        }
        return select(id);
    }

    protected final void rejectDuplicate(T entity, Criteria criteria, String message) {
        if (existsOtherInCurrentScope(entity, criteria)) {
            throw new PlatformException(message);
        }
    }

    protected final void rejectChanged(String fieldName, Object existingValue, Object incomingValue) {
        if (!Objects.equals(existingValue, incomingValue)) {
            throw new PlatformException(fieldName + " cannot be changed");
        }
    }

    protected final <V> void rejectChanged(T existing, T incoming, String fieldName, Function<T, V> valueReader) {
        if (existing != null) {
            rejectChanged(fieldName, valueReader.apply(existing), valueReader.apply(incoming));
        }
    }
}
