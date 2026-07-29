package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraintDefinition;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.util.List;

/** Supplies tenant-scoped unique constraints to the common CRUD write chain. */
public interface TenantUniqueConstraintProvider<T extends EntityContract> {
    List<TenantUniqueConstraintDefinition> tenantUniqueConstraints();

    Object tenantUniqueConstraintValue(T entity, String fieldName);
}
