package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.capability.PlatformManagedCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;

import java.util.Set;

/**
 * Protects platform-managed records from ordinary runtime mutation.
 */
public interface PlatformManagedProtectionAbility<T extends EntityContract & PlatformManagedCapable>
        extends CrudAbility<T> {

    default Set<String> editablePlatformManagedFields() {
        return Set.of(
                PlatformAbilityFields.ENABLED_FIELD,
                PlatformAbilityFields.SORT_FIELD
        );
    }

    default boolean allowOrdinaryPlatformManagedInsert(T entity) {
        return false;
    }
}
