package net.ximatai.muyun.spring.ability.deletion;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/**
 * Per-operation deletion observer.
 *
 * <p>A session is created for a direct deletion and then travels with the
 * explicit {@link DeletionContext}. It deliberately owns no thread-bound
 * state: nested child deletions observe the same operation because they
 * receive the same context.</p>
 */
public interface DeletionLifecycleSession {
    DeletionLifecycleSession NONE = new DeletionLifecycleSession() {
    };

    default DeletionNode started(CrudAbility<?> ability,
                                 EntityContract entity,
                                 DeletionContext context,
                                 DeletionMode mode) {
        return DeletionNode.transientNode(new DeletionResource(ability.getModuleAlias(), entity.getId()));
    }

    default void succeeded(CrudAbility<?> ability,
                           EntityContract entity,
                           DeletionContext context,
                           DeletionNode node,
                           DeletionMode mode) {
    }

    default void failed(CrudAbility<?> ability,
                        EntityContract entity,
                        DeletionContext context,
                        DeletionNode node,
                        DeletionMode mode,
                        RuntimeException failure) {
    }
}
