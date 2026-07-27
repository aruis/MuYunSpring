package net.ximatai.muyun.spring.ability.deletion;

import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

/**
 * A soft-delete resource that can be located again from a persisted deletion entry.
 *
 * <p>The entity alias is part of the stable recovery identity. Soft deletion
 * itself remains independent of recycle-bin and recovery participation.</p>
 */
public interface DeletionRecoveryAbility<T extends EntityContract> extends SoftDeleteAbility<T> {
    String getDeletionEntityAlias();
}
