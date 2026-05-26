package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.SortModel;

public interface SortAbility<T extends SortModel> extends CrudAbility<T> {
    default String getSortField() {
        return "sort_order";
    }
}
