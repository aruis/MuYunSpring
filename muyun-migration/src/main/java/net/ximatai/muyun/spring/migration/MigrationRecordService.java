package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.GlobalScopedAbility;
import net.ximatai.muyun.spring.ability.SystemManagedAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
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

    public MigrationRecordService(MigrationRecordDao dao) {
        super(MODULE_ALIAS, MigrationRecord.class, dao);
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
}
