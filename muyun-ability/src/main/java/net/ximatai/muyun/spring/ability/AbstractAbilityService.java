package net.ximatai.muyun.spring.ability;

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
}
