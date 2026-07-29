package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/** Platform hook that protects a reference target before it is soft-deleted. */
@FunctionalInterface
public interface ReferenceDeletionGuard {
    ReferenceDeletionGuard NONE = (ability, entity) -> { };

    void beforeSoftDelete(CrudAbility<?> ability, EntityContract entity);
}
