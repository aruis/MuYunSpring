package net.ximatai.muyun.spring.ability.child;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/** Identifies a static child model for runtime child-ability resolution. */
public record ChildAbilityRequest(Class<? extends EntityContract> staticModel) {

    public static ChildAbilityRequest forStaticModel(Class<? extends EntityContract> model) {
        return new ChildAbilityRequest(model);
    }
}
