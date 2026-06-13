package net.ximatai.muyun.spring.migration;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.common.schema.StaticSchemaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the data-migration subsystem. {@link MigrationRecordService}, {@link MigrationExecutor}
 * and {@link MigrationBootstrap} are picked up by component scan; this configuration only
 * supplies the infrastructure beans they depend on, each overridable.
 */
@Configuration(proxyBeanMethods = false)
public class MuYunMigrationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    StaticSchemaService migrationStaticSchemaService(IDatabaseOperations<?> operations) {
        return new StaticSchemaService(operations);
    }

    @Bean
    @ConditionalOnMissingBean(MigrationVersionStore.class)
    MigrationVersionStore migrationVersionStore(MigrationRecordService recordService) {
        return new GlobalMigrationVersionStore(recordService);
    }
}
