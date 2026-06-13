package net.ximatai.muyun.spring.migration;

/**
 * Reads and records the applied migration version for a given alias.
 *
 * <p>The method signatures are deliberately tenant-agnostic. Today only a global implementation
 * exists ({@link GlobalMigrationVersionStore}); the tenant dimension is resolved inside the
 * implementation. When per-tenant migration is needed later, a tenant-scoped implementation can
 * be dropped in without changing any caller or migration class.
 */
public interface MigrationVersionStore {

    /** Returns the highest version applied for {@code alias}, or {@code 0} if none recorded yet. */
    int currentVersion(String alias);

    /** Records that {@code version} has been applied for {@code alias}. */
    void recordVersion(String alias, int version);
}
