package net.ximatai.muyun.spring.ability.option;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;

public interface OptionFieldOutputAbility<T extends EntityContract> extends CrudAbility<T> {
    StaticOptionFieldTitlePopulator optionFieldTitlePopulator();

    default T populateOptionTitlesForOutput(T entity) {
        if (entity != null) {
            optionFieldTitlePopulator().populate(modelClass(), entity);
        }
        return entity;
    }

    default List<T> populateOptionTitlesForOutput(List<T> entities) {
        optionFieldTitlePopulator().populateAll(modelClass(), entities);
        return entities;
    }
}
