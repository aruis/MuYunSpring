package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.spring.ability.CacheRegistry;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.reference.ReferenceDependencyRegistry;
import net.ximatai.muyun.spring.ability.security.FieldCryptoProvider;
import net.ximatai.muyun.spring.ability.security.FieldSigner;
import net.ximatai.muyun.spring.common.time.PlatformTimeService;
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
    private final DynamicActionTransactionOperator actionTransactionOperator;
    private final FieldCryptoProvider fieldCryptoProvider;
    private final FieldSigner fieldSigner;
    private final PlatformTimeService timeService;
    private final DatabaseValueConverter valueConverter;

    public DynamicRecordRuntime(IDatabaseOperations<?> operations) {
        this(builder(operations));
    }

    private DynamicRecordRuntime(Builder builder) {
        this.operations = Objects.requireNonNull(builder.operations, "operations must not be null");
        this.registry = Objects.requireNonNull(builder.registry, "registry must not be null");
        this.fieldValueValidator = Objects.requireNonNull(builder.fieldValueValidator,
                "fieldValueValidator must not be null");
        this.eventPublisher = Objects.requireNonNull(builder.eventPublisher, "eventPublisher must not be null");
        this.actionExecutorRegistry = Objects.requireNonNull(builder.actionExecutorRegistry,
                "actionExecutorRegistry must not be null");
        this.actionTransactionOperator = Objects.requireNonNull(builder.actionTransactionOperator,
                "actionTransactionOperator must not be null");
        this.fieldCryptoProvider = Objects.requireNonNull(builder.fieldCryptoProvider,
                "fieldCryptoProvider must not be null");
        this.fieldSigner = Objects.requireNonNull(builder.fieldSigner, "fieldSigner must not be null");
        this.timeService = Objects.requireNonNull(builder.timeService, "timeService must not be null");
        this.valueConverter = Objects.requireNonNull(builder.valueConverter, "valueConverter must not be null");
        this.cacheNamespacePrefix = "dynamic-runtime-" + CACHE_NAMESPACE_SEQUENCE.incrementAndGet();
    }

    public static Builder builder(IDatabaseOperations<?> operations) {
        return new Builder(operations);
    }

    public static final class Builder {
        private final IDatabaseOperations<?> operations;
        private DynamicModuleRegistry registry = new DynamicModuleRegistry();
        private DynamicFieldValueValidator fieldValueValidator = DynamicFieldValueValidator.NONE;
        private RuntimeEventPublisher eventPublisher = RuntimeEventPublisher.noop();
        private DynamicActionExecutorRegistry actionExecutorRegistry = DynamicActionExecutorRegistry.empty();
        private DynamicActionTransactionOperator actionTransactionOperator = DynamicActionTransactionOperator.none();
        private FieldCryptoProvider fieldCryptoProvider = FieldCryptoProvider.UNAVAILABLE;
        private FieldSigner fieldSigner = FieldSigner.UNAVAILABLE;
        private PlatformTimeService timeService = new PlatformTimeService();
        private DatabaseValueConverter valueConverter = DatabaseValueConverter.DEFAULT;

        private Builder(IDatabaseOperations<?> operations) {
            this.operations = Objects.requireNonNull(operations, "operations must not be null");
        }

        public Builder registry(DynamicModuleRegistry registry) {
            this.registry = Objects.requireNonNull(registry, "registry must not be null");
            return this;
        }

        public Builder fieldValueValidator(DynamicFieldValueValidator fieldValueValidator) {
            this.fieldValueValidator = Objects.requireNonNull(fieldValueValidator,
                    "fieldValueValidator must not be null");
            return this;
        }

        public Builder eventPublisher(RuntimeEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher == null ? RuntimeEventPublisher.noop() : eventPublisher;
            return this;
        }

        public Builder actionExecutorRegistry(DynamicActionExecutorRegistry actionExecutorRegistry) {
            this.actionExecutorRegistry = actionExecutorRegistry == null
                    ? DynamicActionExecutorRegistry.empty()
                    : actionExecutorRegistry;
            return this;
        }

        public Builder actionTransactionOperator(DynamicActionTransactionOperator actionTransactionOperator) {
            this.actionTransactionOperator = actionTransactionOperator == null
                    ? DynamicActionTransactionOperator.none()
                    : actionTransactionOperator;
            return this;
        }

        public Builder fieldProtection(FieldCryptoProvider fieldCryptoProvider, FieldSigner fieldSigner) {
            this.fieldCryptoProvider = fieldCryptoProvider == null
                    ? FieldCryptoProvider.UNAVAILABLE
                    : fieldCryptoProvider;
            this.fieldSigner = fieldSigner == null ? FieldSigner.UNAVAILABLE : fieldSigner;
            return this;
        }

        public Builder timeService(PlatformTimeService timeService) {
            this.timeService = timeService == null ? new PlatformTimeService() : timeService;
            return this;
        }

        public Builder valueConverter(DatabaseValueConverter valueConverter) {
            this.valueConverter = valueConverter == null ? DatabaseValueConverter.DEFAULT : valueConverter;
            return this;
        }

        public DynamicRecordRuntime build() {
            return new DynamicRecordRuntime(this);
        }
    }

    public DynamicRecordRuntime register(ModuleDefinition module) {
        registry.register(module);
        return this;
    }

    public DynamicRecordRuntime refresh(ModuleDefinition module) {
        registry.refresh(module);
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

    public DynamicActionTransactionOperator actionTransactionOperator() {
        return actionTransactionOperator;
    }

    public IDatabaseOperations<?> operations() {
        return operations;
    }

    public DynamicEntityService entityService(String moduleAlias, String entityAlias) {
        return entityService(moduleAlias, entityAlias, DynamicRecordLifecycle.NONE);
    }

    public DynamicEntityService entityService(String moduleAlias, String entityAlias, DynamicRecordLifecycle lifecycle) {
        ModuleDefinition module = registry.requireModule(moduleAlias);
        EntityDefinition entity = registry.requireEntity(moduleAlias, entityAlias);
        return new DynamicEntityService(
                new DynamicRecordDao(operations, entity, valueConverter),
                moduleAlias,
                lifecycle,
                module,
                childEntityAliasCode -> entityService(moduleAlias, childEntityAliasCode),
                target -> entityService(target.moduleAlias(), target.entityAlias()),
                cacheNamespacePrefix,
                fieldValueValidator,
                fieldCryptoProvider,
                fieldSigner,
                timeService
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
