package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;

import java.util.Map;
import java.util.Objects;

public class DynamicModulePublisher {
    private final DynamicSchemaService schemaService;
    private final DynamicRecordRuntime runtime;

    public DynamicModulePublisher(DynamicSchemaService schemaService, DynamicRecordRuntime runtime) {
        this.schemaService = Objects.requireNonNull(schemaService, "schemaService must not be null");
        this.runtime = Objects.requireNonNull(runtime, "runtime must not be null");
    }

    public DynamicModulePublishResult publish(ModuleDefinition module) {
        return publish(module, MigrationOptions.execute());
    }

    public DynamicModulePublishResult publish(ModuleDefinition module, MigrationOptions options) {
        runtime.requireNotRegistered(module.moduleAlias());
        Map<String, MigrationResult> migrations = schemaService.ensureModule(module, options);
        if (migrations.values().stream().noneMatch(MigrationResult::isDryRun)) {
            runtime.register(module);
        }
        return new DynamicModulePublishResult(module, migrations);
    }
}
