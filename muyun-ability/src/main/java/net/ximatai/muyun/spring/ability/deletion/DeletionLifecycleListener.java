package net.ximatai.muyun.spring.ability.deletion;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/**
 * Platform extension point for deletion-operation and deletion-entry persistence.
 * Ability remains independent of the platform persistence module through this SPI.
 */
public interface DeletionLifecycleListener {
    DeletionLifecycleListener NONE = new DeletionLifecycleListener() {
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
