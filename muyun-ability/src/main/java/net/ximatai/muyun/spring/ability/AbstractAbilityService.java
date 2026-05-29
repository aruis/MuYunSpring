package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.util.Preconditions;

import java.util.Objects;

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
}
