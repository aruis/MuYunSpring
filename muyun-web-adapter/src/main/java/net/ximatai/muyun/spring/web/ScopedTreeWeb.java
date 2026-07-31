package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.ability.TreeAbility;
import net.ximatai.muyun.spring.common.model.capability.TreeCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/** Legacy endpoint adapter; static modules should implement {@link ScopedTreeWebProjectionPolicy} directly. */
public interface ScopedTreeWeb<T extends EntityContract & TreeCapable, S extends TreeAbility<T>>
        extends TreeWeb<T, S>, ScopedTreeWebProjectionPolicy<T, S> {
}
