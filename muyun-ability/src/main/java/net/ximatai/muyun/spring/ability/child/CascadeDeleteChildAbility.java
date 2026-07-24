package net.ximatai.muyun.spring.ability.child;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.ability.deletion.DeletionContext;
import net.ximatai.muyun.spring.ability.deletion.DeletionNode;

import java.util.Collection;

public interface CascadeDeleteChildAbility<C extends EntityContract> extends ChildAbility<C> {
    default int deleteBatchFromParentCascade(Collection<String> ids) {
        return deleteBatch(ids);
    }

    default int deleteBatchFromParentCascade(Collection<String> ids,
                                             DeletionContext deletionContext,
                                             DeletionNode deletionNode) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String id : ids) {
            count += delete(id, null, deletionContext.child(deletionNode, getModuleAlias(), id));
        }
        return count;
    }
}
