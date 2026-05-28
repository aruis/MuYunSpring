package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.ability.ReferenceAbility;
import net.ximatai.muyun.spring.ability.ReferenceTarget;

final class DynamicReferenceRuntime extends DynamicAbilityRuntime<DynamicTitledRecord> implements ReferenceAbility<DynamicTitledRecord> {
    DynamicReferenceRuntime(DynamicEntityService owner) {
        super(owner, DynamicTitledRecord::new);
    }

    @Override
    public ReferenceTarget referenceTarget() {
        return owner.referenceTarget();
    }

    @Override
    public DynamicTitledRecord selectReferenceRaw(String id) {
        return wrap(owner.activeRaw(id));
    }

    @Override
    public String referenceTitle(DynamicTitledRecord entity) {
        return entity == null ? null : entity.getTitle();
    }
}
