package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.common.schema.StaticSchemaService;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures the version table exists and runs every registered {@link AbstractMigration} once the
 * application context is ready. Runs in a system context so migrations may touch any platform
 * data regardless of tenant.
 *
 * <p>Ordered late ({@code @Order(100)}) so that other startup runners have a chance to register
 * or initialize first.
 */
@Component
@Order(100)
public class MigrationBootstrap implements ApplicationRunner {

    // PostgreSQL-specific partial unique index. The platform targets PostgreSQL (see
    // testcontainers + postgres:16 in build.gradle.kts); supporting another database would
    // require adapting this DDL. IF NOT EXISTS keeps bootstrap idempotent.
    static final String ALIAS_GLOBAL_PARTIAL_INDEX =
            "CREATE UNIQUE INDEX IF NOT EXISTS migration_record_alias_global_unique "
                    + "ON migration_record (alias) WHERE tenant_id IS NULL";

    private static final Logger logger = LoggerFactory.getLogger(MigrationBootstrap.class);

    private final StaticSchemaService schemaService;
    private final IDatabaseOperations<?> operations;
    private final MigrationExecutor executor;
    private final List<AbstractMigration> migrations;

    public MigrationBootstrap(StaticSchemaService schemaService,
                              IDatabaseOperations<?> operations,
                              MigrationExecutor executor,
                              List<AbstractMigration> migrations) {
        this.schemaService = schemaService;
        this.operations = operations;
        this.executor = executor;
        this.migrations = migrations == null ? List.of() : migrations;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (migrations.isEmpty()) {
            return;
        }
        try (TenantContext.Scope ignored = TenantContext.system("data migration on startup")) {
            logger.info("Ensuring migration version table");
            schemaService.ensureTable(MigrationRecord.class);
            ensureAliasGlobalUnique();
            for (AbstractMigration migration : migrations) {
                executor.run(migration);
            }
        }
    }

    // PlatformUniqueIndexes rewrites @Column(unique=true) on alias into a (tenant_id, alias)
    // composite unique index. SQL treats NULL != NULL, so while tenant_id is always null that
    // composite index does NOT enforce alias global uniqueness. This partial index closes the
    // gap. When per-tenant migration lands, a sibling partial index "WHERE tenant_id IS NOT
    // NULL" can be added without conflict.
    private void ensureAliasGlobalUnique() {
        logger.info("Ensuring alias global unique index");
        operations.execute(ALIAS_GLOBAL_PARTIAL_INDEX);
    }
}

