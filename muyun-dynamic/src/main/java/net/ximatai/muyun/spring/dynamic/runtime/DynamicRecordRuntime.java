package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.ability.CacheRegistry;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.reference.ReferenceDependencyRegistry;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class DynamicRecordRuntime implements AutoCloseable {
    private static final AtomicLong CACHE_NAMESPACE_SEQUENCE = new AtomicLong();

    private final IDatabaseOperations<?> operations;
    private final DynamicModuleRegistry registry;
    private final String cacheNamespacePrefix;
    private final DynamicFieldValueValidator fieldValueValidator;
    private final RuntimeEventPublisher eventPublisher;
    private final DynamicActionExecutorRegistry actionExecutorRegistry;

    public DynamicRecordRuntime(IDatabaseOperations<?> operations) {
        this(operations, new DynamicModuleRegistry());
    }

    public DynamicRecordRuntime(IDatabaseOperations<?> operations, DynamicModuleRegistry registry) {
        this(operations, registry, DynamicFieldValueValidator.NONE);
    }

    public DynamicRecordRuntime(IDatabaseOperations<?> operations, DynamicFieldValueValidator fieldValueValidator) {
        this(operations, new DynamicModuleRegistry(), fieldValueValidator);
    }

    public DynamicRecordRuntime(IDatabaseOperations<?> operations,
                                DynamicModuleRegistry registry,
                                DynamicFieldValueValidator fieldValueValidator) {
        this(operations, registry, fieldValueValidator, RuntimeEventPublisher.noop());
    }

    public DynamicRecordRuntime(IDatabaseOperations<?> operations,
                                DynamicModuleRegistry registry,
                                DynamicFieldValueValidator fieldValueValidator,
                                RuntimeEventPublisher eventPublisher) {
        this(operations, registry, fieldValueValidator, eventPublisher, DynamicActionExecutorRegistry.empty());
    }

    public DynamicRecordRuntime(IDatabaseOperations<?> operations,
                                DynamicModuleRegistry registry,
                                DynamicFieldValueValidator fieldValueValidator,
                                RuntimeEventPublisher eventPublisher,
                                DynamicActionExecutorRegistry actionExecutorRegistry) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.fieldValueValidator = Objects.requireNonNull(fieldValueValidator, "fieldValueValidator must not be null");
        this.eventPublisher = eventPublisher == null ? RuntimeEventPublisher.noop() : eventPublisher;
        this.actionExecutorRegistry = actionExecutorRegistry == null
                ? DynamicActionExecutorRegistry.empty()
                : actionExecutorRegistry;
        this.cacheNamespacePrefix = "dynamic-runtime-" + CACHE_NAMESPACE_SEQUENCE.incrementAndGet();
    }

    public DynamicRecordRuntime register(ModuleDefinition module) {
        registry.register(module);
        return this;
    }

    public DynamicRecordRuntime publish(ModuleDefinition module) {
        registry.publish(module);
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

    public DynamicRecord newRecord(String moduleAlias, String entityAlias) {
        return new DynamicRecord(registry.requireEntity(moduleAlias, entityAlias));
    }

    public DynamicModuleDescriptor describe(String moduleAlias) {
        return registry.describe(moduleAlias);
    }

    public RuntimeEventPublisher eventPublisher() {
        return eventPublisher;
    }

    public DynamicActionExecutorRegistry actionExecutorRegistry() {
        return actionExecutorRegistry;
    }

    public DynamicEntityService entityService(String moduleAlias, String entityAlias) {
        return entityService(moduleAlias, entityAlias, DynamicRecordLifecycle.NONE);
    }

    public DynamicEntityService entityService(String moduleAlias, String entityAlias, DynamicRecordLifecycle lifecycle) {
        ModuleDefinition module = registry.requireModule(moduleAlias);
        EntityDefinition entity = registry.requireEntity(moduleAlias, entityAlias);
        return new DynamicEntityService(
                new DynamicRecordDao(operations, entity),
                moduleAlias,
                lifecycle,
                module,
                childEntityAliasCode -> entityService(moduleAlias, childEntityAliasCode),
                cacheNamespacePrefix,
                fieldValueValidator
        );
    }

    public void clearCache() {
        CacheRegistry.clearNamespacePrefix(cacheNamespacePrefix);
        ReferenceDependencyRegistry.clearNamespacePrefix(cacheNamespacePrefix);
    }

    @Override
    public void close() {
        clearCache();
    }
}
