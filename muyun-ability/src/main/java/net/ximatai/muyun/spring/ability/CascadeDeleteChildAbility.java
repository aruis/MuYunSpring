package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.EntityContract;

import java.util.Collection;

public interface CascadeDeleteChildAbility<C extends EntityContract> extends ChildAbility<C> {
    default int deleteBatchFromParentCascade(Collection<String> ids) {
        return deleteBatch(ids);
    }
}
