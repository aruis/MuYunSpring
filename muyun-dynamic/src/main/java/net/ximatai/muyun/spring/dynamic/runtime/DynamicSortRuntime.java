package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.SortPartition;
import net.ximatai.muyun.database.core.orm.Criteria;

final class DynamicSortRuntime extends DynamicAbilityRuntime<DynamicSortRecord> implements SortAbility<DynamicSortRecord> {
    DynamicSortRuntime(DynamicEntityService owner) {
        super(owner, DynamicSortRecord::new);
    }

    @Override
    public SortPartition<DynamicSortRecord> sortPartition() {
        return new SortPartition<>() {
            @Override
            public Criteria criteriaFor(DynamicSortRecord record) {
                return owner.sortPartition().criteriaFor(record.record());
            }

            @Override
            public void requireSamePartition(DynamicSortRecord left, DynamicSortRecord right) {
                owner.sortPartition().requireSamePartition(left.record(), right.record());
            }
        };
    }
}
