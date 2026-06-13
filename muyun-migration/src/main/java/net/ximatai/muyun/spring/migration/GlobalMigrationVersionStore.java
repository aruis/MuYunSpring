package net.ximatai.muyun.spring.migration;

/**
 * Global (platform-level) implementation of {@link MigrationVersionStore}: every migration alias
 * shares one global version sequence. This is the default. A tenant-scoped implementation can
 * replace it by providing its own {@link MigrationVersionStore} bean.
 */
public class GlobalMigrationVersionStore implements MigrationVersionStore {

    private final MigrationRecordService recordService;

    public GlobalMigrationVersionStore(MigrationRecordService recordService) {
        this.recordService = recordService;
    }

    @Override
    public int currentVersion(String alias) {
        MigrationRecord record = recordService.findByAlias(alias);
        return record == null ? 0 : record.getAppliedVersion();
    }

    @Override
    public void recordVersion(String alias, int version) {
        MigrationRecord record = recordService.findByAlias(alias);
        if (record == null) {
            MigrationRecord created = new MigrationRecord();
            created.setAlias(alias);
            created.setAppliedVersion(version);
            recordService.insert(created);
        } else {
            record.setAppliedVersion(version);
            recordService.update(record);
        }
    }
}
