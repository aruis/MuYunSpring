package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class DynamicRecordRuntime {
    private static final AtomicLong CACHE_NAMESPACE_SEQUENCE = new AtomicLong();

    private final IDatabaseOperations<?> operations;
    private final DynamicModuleRegistry registry;
    private final String cacheNamespacePrefix;

    public DynamicRecordRuntime(IDatabaseOperations<?> operations) {
        this(operations, new DynamicModuleRegistry());
    }

    public DynamicRecordRuntime(IDatabaseOperations<?> operations, DynamicModuleRegistry registry) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.cacheNamespacePrefix = "dynamic-runtime-" + CACHE_NAMESPACE_SEQUENCE.incrementAndGet();
    }

    public DynamicRecordRuntime register(ModuleDefinition module) {
        registry.register(module);
        return this;
    }

    public void requireNotRegistered(String moduleAlias) {
        if (registry.containsModule(moduleAlias)) {
            throw new ModuleDefinitionException("duplicate module alias: " + moduleAlias);
        }
    }

    public DynamicModuleRegistry registry() {
        return registry;
    }

    public DynamicRecord newRecord(String moduleAlias, String entityCode) {
        return new DynamicRecord(registry.requireEntity(moduleAlias, entityCode));
    }

    public DynamicEntityService entityService(String moduleAlias, String entityCode) {
        return entityService(moduleAlias, entityCode, DynamicRecordLifecycle.NONE);
    }

    public DynamicEntityService entityService(String moduleAlias, String entityCode, DynamicRecordLifecycle lifecycle) {
        ModuleDefinition module = registry.requireModule(moduleAlias);
        EntityDefinition entity = registry.requireEntity(moduleAlias, entityCode);
        return new DynamicEntityService(
                new DynamicRecordDao(operations, entity),
                moduleAlias,
                lifecycle,
                module,
                childEntityCode -> entityService(moduleAlias, childEntityCode),
                cacheNamespacePrefix
        );
    }
}
