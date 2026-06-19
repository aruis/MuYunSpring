package net.ximatai.muyun.spring.ability.initialdata;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;

public interface InitialDataAbility<T extends EntityContract> extends CrudAbility<T> {
    List<T> initialData();

    default InitialDataOptions initialDataOptions() {
        return InitialDataOptions.defaults();
    }
}
