package net.ximatai.muyun.spring.migration;

/**
 * One versioned migration step. Versions must be positive and unique within a single migration.
 */
public record MigrateStep(Integer version, MigrateAction action) {

    public MigrateStep {
        if (version == null || version <= 0) {
            throw new IllegalArgumentException("migration version must be greater than 0: " + version);
        }
        if (action == null) {
            throw new IllegalArgumentException("migration action must not be null");
        }
    }
}
