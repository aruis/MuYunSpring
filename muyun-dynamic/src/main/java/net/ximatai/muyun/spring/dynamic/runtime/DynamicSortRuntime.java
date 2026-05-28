package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.ability.SortAbility;

final class DynamicSortRuntime extends DynamicAbilityRuntime<DynamicSortRecord> implements SortAbility<DynamicSortRecord> {
    DynamicSortRuntime(DynamicEntityService owner) {
        super(owner, DynamicSortRecord::new);
    }
}
