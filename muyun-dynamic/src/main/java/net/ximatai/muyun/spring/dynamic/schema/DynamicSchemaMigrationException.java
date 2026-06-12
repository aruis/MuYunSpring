package net.ximatai.muyun.spring.dynamic.schema;

import net.ximatai.muyun.database.core.orm.MigrationResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class DynamicSchemaMigrationException extends RuntimeException {
    private final String moduleAlias;
    private final String failedEntityAlias;
    private final Map<String, MigrationResult> completedMigrations;

    public DynamicSchemaMigrationException(String moduleAlias,
                                           String failedEntityAlias,
                                           Map<String, MigrationResult> completedMigrations,
                                           Throwable cause) {
        super("dynamic schema migration failed: " + moduleAlias + "." + failedEntityAlias, cause);
        this.moduleAlias = moduleAlias;
        this.failedEntityAlias = failedEntityAlias;
        this.completedMigrations = completedMigrations == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(completedMigrations));
    }

    public String moduleAlias() {
        return moduleAlias;
    }

    public String failedEntityAlias() {
        return failedEntityAlias;
    }

    public Map<String, MigrationResult> completedMigrations() {
        return completedMigrations;
    }
}
