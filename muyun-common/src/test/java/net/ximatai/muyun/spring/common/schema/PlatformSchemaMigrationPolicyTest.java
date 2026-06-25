package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformSchemaMigrationPolicyTest {
    @Test
    void shouldUseExecuteByDefaultWhenRuntimeModeProviderIsMissing() {
        MigrationOptions options = PlatformSchemaMigrationPolicy.executeByDefault().defaultOptions();

        assertThat(options.isDryRun()).isFalse();
        assertThat(options.isStrict()).isFalse();
    }

    @Test
    void shouldUseExecuteByDefaultInDevelopmentMode() {
        PlatformSchemaMigrationPolicy policy = new PlatformSchemaMigrationPolicy(() -> PlatformRuntimeMode.DEVELOPMENT);

        MigrationOptions options = policy.defaultOptions();

        assertThat(options.isDryRun()).isFalse();
        assertThat(options.isStrict()).isFalse();
    }

    @Test
    void shouldUseStrictByDefaultInProductionMode() {
        PlatformSchemaMigrationPolicy policy = new PlatformSchemaMigrationPolicy(() -> PlatformRuntimeMode.PRODUCTION);

        MigrationOptions options = policy.defaultOptions();

        assertThat(options.isDryRun()).isFalse();
        assertThat(options.isStrict()).isTrue();
    }

    @Test
    void shouldKeepExplicitMigrationOptions() {
        PlatformSchemaMigrationPolicy policy = new PlatformSchemaMigrationPolicy(() -> PlatformRuntimeMode.PRODUCTION);

        MigrationOptions options = policy.resolve(MigrationOptions.dryRun());

        assertThat(options.isDryRun()).isTrue();
        assertThat(options.isStrict()).isFalse();
    }
}
