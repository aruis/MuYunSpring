package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.ability.TreeAbility;

final class DynamicTreeRuntime extends DynamicAbilityRuntime<DynamicTreeRecord> implements TreeAbility<DynamicTreeRecord> {
    DynamicTreeRuntime(DynamicEntityService owner) {
        super(owner, DynamicTreeRecord::new);
    }
}
