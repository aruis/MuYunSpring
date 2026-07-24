package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;

import java.util.List;

/**
 * Optional lifecycle-management capability layered on top of soft deletion.
 *
 * <p>Soft deletion only retains a record. A service implements this ability
 * only when it deliberately exposes retained records to operators through a
 * recycle-bin lifecycle. Recovery execution and lifecycle audit remain
 * platform concerns.</p>
 */
public interface RecycleBinAbility<T extends EntityContract> extends SoftDeleteAbility<T> {
    /** Lists retained records visible to the current recycle-bin boundary. */
    default List<T> listRecycleBin(PageRequest pageRequest) {
        beforeRecycleBinQuery();
        PageRequest effectivePage = pageRequest == null ? PageRequest.of(1, 20) : pageRequest;
        return getDao().query(recycleBinCriteria(Criteria.of()
                .eq(StandardEntitySchema.DELETED_FIELD, Boolean.TRUE)), effectivePage);
    }

    /**
     * Allows scoped services to add their normal recycle-bin visibility rules.
     * The default keeps the ordinary tenant filter, but intentionally does not
     * reuse {@link #activeCriteria(Criteria)} because active records and
     * retained records have opposite deleted-state predicates.
     */
    default Criteria recycleBinCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        if (!TenantContext.tenantFilterBypassed()) {
            TenantContext.currentTenantId()
                    .ifPresent(tenantId -> scoped.eq(StandardEntitySchema.TENANT_ID_FIELD, tenantId));
        }
        return scoped;
    }

    /** Hook for the resource owner to enforce its query boundary. */
    default void beforeRecycleBinQuery() {
    }

    /** Hook for the resource owner to enforce its recovery boundary. */
    default void beforeRecycleBinRestore() {
    }
}
