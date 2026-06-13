package net.ximatai.muyun.spring.migration;

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

    private static final Logger logger = LoggerFactory.getLogger(MigrationBootstrap.class);

    private final StaticSchemaService schemaService;
    private final MigrationExecutor executor;
    private final List<AbstractMigration> migrations;

    public MigrationBootstrap(StaticSchemaService schemaService,
                              MigrationExecutor executor,
                              List<AbstractMigration> migrations) {
        this.schemaService = schemaService;
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
            for (AbstractMigration migration : migrations) {
                executor.run(migration);
            }
        }
    }
}
