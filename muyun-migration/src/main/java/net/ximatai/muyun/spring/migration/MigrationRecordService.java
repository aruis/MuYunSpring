package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.GlobalScopedAbility;
import net.ximatai.muyun.spring.ability.SystemManagedAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.id.Ids;
import org.springframework.stereotype.Service;

/**
 * Manages {@link MigrationRecord} entries. This is a global, system-managed table: it does not
 * belong to any tenant and may only be written from a system context (which the migration
 * bootstrap guarantees).
 */
@Service
public class MigrationRecordService extends AbstractAbilityService<MigrationRecord> implements
        SystemManagedAbility<MigrationRecord>,
        GlobalScopedAbility<MigrationRecord> {

    public static final String MODULE_ALIAS = "platform.migration";

    // PostgreSQL-specific upsert. The partial unique index `migration_record_alias_global_unique`
    // (created by MigrationBootstrap) is the conflict arbiter: when an alias already exists
    // under tenant_id IS NULL, the INSERT side is rejected and the DO UPDATE branch runs,
    // advancing applied_version atomically. This closes the multi-instance concurrent-bootstrap
    // race without requiring a distributed lock.
    static final String UPSERT_ALIAS_VERSION_SQL =
            "INSERT INTO migration_record (id, tenant_id, alias, applied_version, version, deleted, created_at, updated_at) "
                    + "VALUES (?, NULL, ?, ?, 0, false, now(), now()) "
                    + "ON CONFLICT (alias) WHERE tenant_id IS NULL "
                    + "DO UPDATE SET applied_version = EXCLUDED.applied_version, updated_at = now()";

    private final IDatabaseOperations<?> operations;

    public MigrationRecordService(MigrationRecordDao dao, IDatabaseOperations<?> operations) {
        super(MODULE_ALIAS, MigrationRecord.class, dao);
        this.operations = operations;
    }

    @Override
    public void normalizeBeforeMutation(MigrationRecord record) {
        record.setTenantId(null);
        if (record.getAlias() == null || record.getAlias().isBlank()) {
            throw new PlatformException("migration alias must not be blank");
        }
    }

    MigrationRecord findByAlias(String alias) {
        return findOne(Criteria.of().eq("alias", alias));
    }

    /**
     * Atomically insert-or-update the applied version for an alias. Bypasses the CrudAbility
     * insert/update path because those do not surface a conflict-resolution hook for the
     * alias-scoped partial unique index; the raw SQL is the cleanest way to express the
     * single-statement upsert the race requires.
     */
    void upsertAliasVersion(String alias, int version) {
        operations.execute(UPSERT_ALIAS_VERSION_SQL, Ids.newId(), alias, version);
    }
}

