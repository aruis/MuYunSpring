package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/** Resolves typed {@link ReferenceLoad} paths after static or dynamic compilation. */
@FunctionalInterface
public interface ReferenceLoadResolver {
    ReferenceLoadResolver NONE = (ability, entity) -> { };

    void populate(CrudAbility<?> ability, EntityContract entity);
}
