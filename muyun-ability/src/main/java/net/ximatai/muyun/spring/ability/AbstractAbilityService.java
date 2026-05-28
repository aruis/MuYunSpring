package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.Objects;

public abstract class AbstractAbilityService<T extends EntityContract> implements CrudAbility<T> {
    private final String moduleAlias;
    private final BaseDao<T, String> dao;

    protected AbstractAbilityService(String moduleAlias, BaseDao<T, String> dao) {
        this.moduleAlias = requireText(moduleAlias, "moduleAlias");
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

    private static String requireText(String value, String name) {
        String text = Objects.requireNonNull(value, name + " must not be null").trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return text;
    }
}
