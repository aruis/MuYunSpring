package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Runs a single {@link AbstractMigration}: sorts and validates its steps, then applies only those
 * newer than the recorded version, each inside its own transaction.
 */
@Component
public class MigrationExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MigrationExecutor.class);

    private final MigrationVersionStore versionStore;
    private final TransactionTemplate transactionTemplate;

    public MigrationExecutor(MigrationVersionStore versionStore, PlatformTransactionManager transactionManager) {
        this.versionStore = versionStore;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void run(AbstractMigration migration) {
        try (TenantContext.Scope ignored = TenantContext.system("data migration: " + migration.getAlias())) {
            String alias = migration.getAlias();
            List<MigrateStep> steps = sortedAndValidated(alias, migration.getMigrateSteps());

            if (steps.isEmpty()) {
                logger.info("Migration '{}' has no steps", alias);
                return;
            }

            int finalVersion = steps.getLast().version();
            int currentVersion = versionStore.currentVersion(alias);
            if (currentVersion >= finalVersion) {
                logger.info("Migration '{}' already at version {}, nothing to do", alias, currentVersion);
                return;
            }

            logger.info("Migration '{}' starting (current {}, target {})", alias, currentVersion, finalVersion);
            for (MigrateStep step : steps) {
                if (step.version() <= currentVersion) {
                    continue;
                }
                logger.info("Migration '{}' applying step {}", alias, step.version());
                transactionTemplate.executeWithoutResult(status -> {
                    step.action().migrate();
                    versionStore.recordVersion(alias, step.version());
                });
                currentVersion = step.version();
                logger.info("Migration '{}' applied step {}", alias, step.version());
            }
            logger.info("Migration '{}' completed (reached {})", alias, finalVersion);
        }
    }

    private List<MigrateStep> sortedAndValidated(String alias, List<MigrateStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return List.of();
        }
        List<MigrateStep> sorted = new ArrayList<>(steps);
        sorted.sort(Comparator.comparingInt(MigrateStep::version));
        HashSet<Integer> seen = new HashSet<>();
        for (MigrateStep step : sorted) {
            if (!seen.add(step.version())) {
                throw new PlatformException("duplicate migration version " + step.version() + " in '" + alias + "'");
            }
        }
        return sorted;
    }
}
