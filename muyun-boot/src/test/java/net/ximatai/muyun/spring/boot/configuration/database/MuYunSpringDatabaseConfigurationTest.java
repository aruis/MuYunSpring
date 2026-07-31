package net.ximatai.muyun.spring.boot.configuration.database;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.spring.common.schema.PlatformSchemaMigrationPolicy;
import net.ximatai.muyun.spring.common.schema.StaticSchemaService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MuYunSpringDatabaseConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MuYunSpringDatabaseConfiguration.class)
            .withBean(IDatabaseOperations.class, () -> mock(IDatabaseOperations.class));

    @Test
    void shouldConfigureStaticSchemaServiceWithProductionStrictDefault() {
        contextRunner.run(context -> {
            PlatformSchemaMigrationPolicy policy = migrationPolicy(context.getBean(StaticSchemaService.class));

            MigrationOptions options = policy.defaultOptions();

            assertThat(options.isStrict()).isTrue();
            assertThat(options.isDryRun()).isFalse();
        });
    }

    @Test
    void shouldConfigureStaticSchemaServiceWithDevelopmentExecuteDefault() {
        contextRunner.withPropertyValues("muyun.runtime.mode=development")
                .run(context -> {
                    PlatformSchemaMigrationPolicy policy = migrationPolicy(context.getBean(StaticSchemaService.class));

                    MigrationOptions options = policy.defaultOptions();

                    assertThat(options.isStrict()).isFalse();
                    assertThat(options.isDryRun()).isFalse();
                });
    }

    private PlatformSchemaMigrationPolicy migrationPolicy(StaticSchemaService schemaService) {
        try {
            Field field = StaticSchemaService.class.getDeclaredField("migrationPolicy");
            field.setAccessible(true);
            return (PlatformSchemaMigrationPolicy) field.get(schemaService);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("StaticSchemaService migration policy is not inspectable", e);
        }
    }
}
