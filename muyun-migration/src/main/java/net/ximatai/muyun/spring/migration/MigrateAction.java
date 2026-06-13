package net.ximatai.muyun.spring.migration;

/** A single migration action, executed within its own transaction. */
@FunctionalInterface
public interface MigrateAction {
    void migrate();
}
