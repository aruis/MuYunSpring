package net.ximatai.muyun.spring.boot.web;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.capability.SortCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.platform.PlatformAction;

public abstract class NestedEnabledSortableCrudWebSupport<
        T extends EntityContract & EnabledCapable & SortCapable,
        S extends CrudAbility<T> & EnableAbility<T> & SortAbility<T>>
        extends NestedCrudWebSupport<T, S> implements RecordWebProjectionPolicy {

    @Override
    public void requireRecord(HttpServletRequest request, PlatformAction action, String id) {
        requireScopedRecord(request, id);
    }
}
