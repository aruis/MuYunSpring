package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventMulticaster;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.ability.security.FieldCryptoProvider;
import net.ximatai.muyun.spring.ability.security.FieldSigner;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllActionExecutionPolicyService;
import net.ximatai.muyun.spring.common.platform.AllowAllDataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaService;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopeResolver;
import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;
import net.ximatai.muyun.spring.common.schema.PlatformSchemaMigrationPolicy;
import net.ximatai.muyun.spring.common.time.BusinessCalendarService;
import net.ximatai.muyun.spring.common.time.BusinessTimeZoneResolver;
import net.ximatai.muyun.spring.common.time.NaturalBusinessCalendarService;
import net.ximatai.muyun.spring.common.time.PlatformTimeService;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionTransactionOperator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.refresh.DynamicModuleRuntimeRefresher;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceDependencyScopeResolver;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordMutationCoordinators;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.schema.DynamicSchemaService;
import net.ximatai.muyun.spring.dynamic.schema.DynamicTableMapper;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryFieldValueValidator;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import io.quarkus.arc.DefaultBean;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import net.ximatai.muyun.spring.common.di.ObjectProvider;

import java.time.Clock;
import java.time.ZoneId;

@ApplicationScoped
public class MuYunSpringDynamicRuntimeConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    DynamicFieldValueValidator dictionaryFieldValueValidator(DictionaryItemService itemService) {
        return new DictionaryFieldValueValidator(itemService);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    PlatformTimeService platformTimeService(ObjectProvider<Clock> clockProvider,
                                            ObjectProvider<BusinessTimeZoneResolver> zoneResolvers,
                                            MuYunSpringPlatformTimeProperties timeProperties) {
        Clock clock = clockProvider == null ? null : clockProvider.getIfAvailable();
        ZoneId defaultZoneId = defaultZoneId(timeProperties);
        return new PlatformTimeService(
                clock,
                defaultZoneId,
                zoneResolvers == null ? null : zoneResolvers.orderedStream().toList()
        );
    }

    private ZoneId defaultZoneId(MuYunSpringPlatformTimeProperties timeProperties) {
        String configured = timeProperties == null ? null : timeProperties.getDefaultZoneId();
        if (configured == null || configured.isBlank()) {
            return null;
        }
        return PlatformTimeService.requireIanaZoneId(configured);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    BusinessCalendarService businessCalendarService(PlatformTimeService platformTimeService) {
        return new NaturalBusinessCalendarService(platformTimeService);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    ModuleDefinitionValidator moduleDefinitionValidator() {
        return new ModuleDefinitionValidator();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    DynamicSchemaService dynamicSchemaService(IDatabaseOperations operations,
                                              ModuleDefinitionValidator moduleDefinitionValidator,
                                              PlatformRuntimeModeProvider runtimeModeProvider) {
        return new DynamicSchemaService(operations, new DynamicTableMapper(), moduleDefinitionValidator,
                new PlatformSchemaMigrationPolicy(runtimeModeProvider));
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    DynamicRecordRuntime dynamicRecordRuntime(IDatabaseOperations operations,
                                              DynamicFieldValueValidator fieldValueValidator,
                                              RuntimeEventPublisher eventPublisher,
                                              DynamicActionExecutorRegistry actionExecutorRegistry,
                                              DynamicActionTransactionOperator actionTransactionOperator,
                                              ObjectProvider<FieldCryptoProvider> fieldCryptoProvider,
                                              ObjectProvider<FieldSigner> fieldSigner,
                                              PlatformTimeService platformTimeService,
                                              DatabaseValueConverter databaseValueConverter) {
        return new DynamicRecordRuntime(operations, new DynamicModuleRegistry(), fieldValueValidator,
                eventPublisher, actionExecutorRegistry, actionTransactionOperator,
                fieldCryptoProvider.getIfAvailable(() -> FieldCryptoProvider.UNAVAILABLE),
                fieldSigner.getIfAvailable(() -> FieldSigner.UNAVAILABLE),
                platformTimeService,
                databaseValueConverter);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    DynamicRecordService dynamicRecordService(DynamicRecordRuntime runtime,
                                              ObjectProvider<ActionExecutionPolicyService> actionExecutionPolicyService,
                                              ObjectProvider<DataScopeCriteriaService> dataScopeCriteriaService,
                                              ObjectProvider<DynamicRecordMutationCoordinator> mutationCoordinator) {
        return new DynamicRecordService(runtime,
                actionExecutionPolicyService.getIfAvailable(AllowAllActionExecutionPolicyService::new),
                dataScopeCriteriaService.getIfAvailable(AllowAllDataScopeCriteriaService::new),
                DynamicRecordMutationCoordinators.lazyComposite(() -> mutationCoordinator.orderedStream().toList()));
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    ReferenceDependencyScopeResolver dynamicReferenceDependencyScopeResolver(DynamicRecordRuntime runtime) {
        return new DynamicReferenceDependencyScopeResolver(runtime);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    DynamicActionExecutorRegistry dynamicActionExecutorRegistry(ObjectProvider<DynamicActionExecutor> executors) {
        return new DynamicActionExecutorRegistry(() -> executors.orderedStream().toList());
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    DynamicActionTransactionOperator dynamicActionTransactionOperator() {
        return (context, action) -> QuarkusTransaction.joiningExisting().call(action::get);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    RuntimeEventPublisher runtimeEventPublisher(ObjectProvider<RuntimeEventListener> listeners) {
        return new RuntimeEventMulticaster(() -> listeners.orderedStream().toList());
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    DynamicModuleRuntimeRefresher dynamicModuleRuntimeRefresher(DynamicSchemaService schemaService,
                                                  DynamicRecordRuntime runtime) {
        return new DynamicModuleRuntimeRefresher(schemaService, runtime);
    }
}
