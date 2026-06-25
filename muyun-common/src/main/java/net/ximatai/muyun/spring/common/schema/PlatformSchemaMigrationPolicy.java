package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeMode;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;

/**
 * Resolves default schema migration behavior from the platform runtime mode.
 */
public final class PlatformSchemaMigrationPolicy {
    private final PlatformRuntimeModeProvider runtimeModeProvider;

    public PlatformSchemaMigrationPolicy(PlatformRuntimeModeProvider runtimeModeProvider) {
        this.runtimeModeProvider = runtimeModeProvider;
    }

    public static PlatformSchemaMigrationPolicy executeByDefault() {
        return new PlatformSchemaMigrationPolicy(null);
    }

    public MigrationOptions defaultOptions() {
        if (runtimeModeProvider != null && runtimeModeProvider.currentMode() == PlatformRuntimeMode.PRODUCTION) {
            return MigrationOptions.strict();
        }
        return MigrationOptions.execute();
    }

    public MigrationOptions resolve(MigrationOptions options) {
        return options == null ? defaultOptions() : options;
    }
}
