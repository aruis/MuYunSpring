package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.common.platform.PlatformAction;

/** Stable record-range fork for standard and retained-record list reads. */
public enum RecordReadVisibility {
    ACTIVE(PlatformAction.QUERY) {
        @Override
        public Criteria apply(CrudAbility<?> ability, Criteria criteria) {
            return ability.activeCriteria(criteria);
        }
    },
    RETAINED(PlatformAction.RECYCLE_BIN_QUERY) {
        @Override
        public Criteria apply(CrudAbility<?> ability, Criteria criteria) {
            if (!(ability instanceof RecycleBinAbility<?> recycleBinAbility)) {
                throw new IllegalArgumentException("retained record reads require RecycleBinAbility: "
                        + ability.getModuleAlias());
            }
            return recycleBinAbility.recycleBinReadCriteria(criteria);
        }
    };

    private final PlatformAction action;

    RecordReadVisibility(PlatformAction action) {
        this.action = action;
    }

    public PlatformAction action() {
        return action;
    }

    public abstract Criteria apply(CrudAbility<?> ability, Criteria criteria);
}
