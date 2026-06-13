package net.ximatai.muyun.spring.dynamic.publish;

import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.schema.DynamicSchemaService;

import java.util.LinkedHashMap;
import java.util.List;
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

    public DynamicModulePublishResult preview(ModuleDefinition module) {
        return publish(module, MigrationOptions.dryRun());
    }

    public DynamicModulePublishResult publish(ModuleDefinition module, MigrationOptions options) {
        MigrationOptions safeOptions = options == null ? MigrationOptions.execute() : options;
        ModuleDefinition previousModule = runtime.registry().findModule(module.moduleAlias()).orElse(null);
        Map<String, MigrationResult> migrations = schemaService.ensureModule(module, previousModule, safeOptions);
        if (!safeOptions.isDryRun()) {
            runtime.publish(module);
            publishModuleEvent(module, migrations);
        }
        return new DynamicModulePublishResult(module, migrations, safeOptions.isDryRun());
    }

    private void publishModuleEvent(ModuleDefinition module, Map<String, MigrationResult> migrations) {
        DynamicModulePublishResult result = new DynamicModulePublishResult(module, migrations, false);
        String systemReason = TenantContext.systemReason().orElse("dynamic module publication");
        runtime.eventPublisher().publishAfterCommit(RuntimeEvent.of(
                RuntimeEventType.MODULE_PUBLISHED,
                module.moduleAlias(),
                null,
                null,
                null,
                TenantContext.currentTenantId().orElse(null),
                true,
                systemReason,
                RuntimeMutationSource.SYSTEM,
                Map.of(
                        "changed", result.changed(),
                        "entities", result.migrations().entrySet().stream()
                                .map(entry -> Map.of(
                                        "entityAlias", entry.getKey(),
                                        "changed", entry.getValue().isChanged(),
                                        "dryRun", entry.getValue().isDryRun(),
                                        "nonAdditiveChanges", entry.getValue().hasNonAdditiveChanges(),
                                        "statements", entry.getValue().getStatements(),
                                        "changes", migrationChanges(entry.getValue())
                                ))
                                .toList(),
                        "nonAdditiveChanges", result.hasNonAdditiveChanges()
                )
        ));
    }

    private List<Map<String, Object>> migrationChanges(MigrationResult result) {
        return result.getChanges().stream()
                .map(change -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("type", change.getType().name());
                    payload.put("target", change.getTarget());
                    payload.put("sql", change.getSql());
                    payload.put("nonAdditive", change.isNonAdditive());
                    return payload;
                })
                .toList();
    }
}
