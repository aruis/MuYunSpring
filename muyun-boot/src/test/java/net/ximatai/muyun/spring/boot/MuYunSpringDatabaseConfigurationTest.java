package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;
import net.ximatai.muyun.spring.common.schema.PlatformSchemaMigrationPolicy;
import net.ximatai.muyun.spring.common.schema.StaticSchemaService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MuYunSpringDatabaseConfigurationTest {
    private final MuYunSpringDatabaseConfiguration configuration = new MuYunSpringDatabaseConfiguration();

    @Test
    void shouldConfigureStaticSchemaServiceWithProductionStrictDefault() {
        StaticSchemaService service = configuration.staticSchemaService(
                mock(IDatabaseOperations.class),
                modeProvider(PlatformRuntimeMode.PRODUCTION)
        );

        MigrationOptions options = migrationPolicy(service).defaultOptions();

        assertThat(options.isStrict()).isTrue();
        assertThat(options.isDryRun()).isFalse();
    }

    @Test
    void shouldConfigureStaticSchemaServiceWithDevelopmentExecuteDefault() {
        StaticSchemaService service = configuration.staticSchemaService(
                mock(IDatabaseOperations.class),
                modeProvider(PlatformRuntimeMode.DEVELOPMENT)
        );

        MigrationOptions options = migrationPolicy(service).defaultOptions();

        assertThat(options.isStrict()).isFalse();
        assertThat(options.isDryRun()).isFalse();
    }

    private PlatformRuntimeModeProvider modeProvider(PlatformRuntimeMode mode) {
        return () -> mode;
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
