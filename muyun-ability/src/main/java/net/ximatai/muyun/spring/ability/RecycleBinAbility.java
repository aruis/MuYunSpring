package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.ability.deletion.DeletionRecoveryAbility;

import java.util.List;

/**
 * Optional lifecycle-management capability layered on top of soft deletion.
 *
 * <p>Soft deletion only retains a record. A service implements this ability
 * only when it deliberately exposes retained records to operators through a
 * recycle-bin lifecycle. Recovery execution and lifecycle audit remain
 * platform concerns.</p>
 */
public interface RecycleBinAbility<T extends EntityContract> extends SoftDeleteAbility<T>, DeletionRecoveryAbility<T> {
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

    /**
     * Physically removes one retained resource after its recycle-bin policy allows it.
     * This primitive deliberately does not infer or traverse children; the platform
     * purge coordinator owns source-tree validation, ordering and audit entries.
     */
    default int purge(String id) {
        if (id == null || id.isBlank()) {
            return 0;
        }
        beforeRecycleBinPurge(id);
        T entity = selectIgnoreSoftDelete(id);
        if (entity == null || !Boolean.TRUE.equals(entity.getDeleted())) {
            return 0;
        }
        int purged = getDao().deleteByIdAndVersion(id, entity.getVersion());
        if (purged <= 0) {
            throw new OptimisticLockException("record version conflict: " + id);
        }
        afterRecycleBinPurge(id, entity, purged);
        afterChanged(entity);
        CacheInvalidationSupport.clearAfterChanged(this, entity);
        return purged;
    }

    /** Defaults to deny: irreversible deletion requires an explicit business decision. */
    default void beforeRecycleBinPurge(String id) {
        throw new UnsupportedOperationException("Recycle-bin purge is not enabled for " + getModuleAlias());
    }

    default void afterRecycleBinPurge(String id, T entity, int purged) {
    }
}
