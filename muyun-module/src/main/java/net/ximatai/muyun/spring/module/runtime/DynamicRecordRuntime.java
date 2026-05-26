package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;

import java.util.Objects;

public class DynamicRecordRuntime {
    private final IDatabaseOperations<?> operations;
    private final DynamicModuleRegistry registry;

    public DynamicRecordRuntime(IDatabaseOperations<?> operations) {
        this(operations, new DynamicModuleRegistry());
    }

    public DynamicRecordRuntime(IDatabaseOperations<?> operations, DynamicModuleRegistry registry) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    public DynamicRecordRuntime register(ModuleDefinition module) {
        registry.register(module);
        return this;
    }

    public DynamicModuleRegistry registry() {
        return registry;
    }

    public DynamicRecord newRecord(String moduleAlias, String entityCode) {
        return new DynamicRecord(registry.requireEntity(moduleAlias, entityCode));
    }

    public DynamicRecordAbility ability(String moduleAlias, String entityCode) {
        return ability(moduleAlias, entityCode, DynamicRecordLifecycle.NONE);
    }

    public DynamicRecordAbility ability(String moduleAlias, String entityCode, DynamicRecordLifecycle lifecycle) {
        EntityDefinition entity = registry.requireEntity(moduleAlias, entityCode);
        return new DynamicRecordAbility(new DynamicRecordDao(operations, entity), moduleAlias, lifecycle);
    }
}
