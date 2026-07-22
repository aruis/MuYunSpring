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
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MuYunSpringPlatformTimeProperties.class)
@Import({MuYunSpringDatabaseValueConversionConfiguration.class, MuYunSpringRuntimeConfiguration.class})
public class MuYunSpringDynamicRuntimeConfiguration {
    @Bean
    @ConditionalOnBean(DictionaryItemService.class)
    @ConditionalOnMissingBean(DynamicFieldValueValidator.class)
    DynamicFieldValueValidator dictionaryFieldValueValidator(DictionaryItemService itemService) {
        return new DictionaryFieldValueValidator(itemService);
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicFieldValueValidator dynamicFieldValueValidator() {
        return DynamicFieldValueValidator.NONE;
    }

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @ConditionalOnMissingBean
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

    @Bean
    @ConditionalOnMissingBean
    BusinessCalendarService businessCalendarService(PlatformTimeService platformTimeService) {
        return new NaturalBusinessCalendarService(platformTimeService);
    }

    @Bean
    @ConditionalOnMissingBean
    ModuleDefinitionValidator moduleDefinitionValidator() {
        return new ModuleDefinitionValidator();
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicSchemaService dynamicSchemaService(IDatabaseOperations<?> operations,
                                              ModuleDefinitionValidator moduleDefinitionValidator,
                                              PlatformRuntimeModeProvider runtimeModeProvider) {
        return new DynamicSchemaService(operations, new DynamicTableMapper(), moduleDefinitionValidator,
                new PlatformSchemaMigrationPolicy(runtimeModeProvider));
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicRecordRuntime dynamicRecordRuntime(IDatabaseOperations<?> operations,
                                              DynamicFieldValueValidator fieldValueValidator,
                                              RuntimeEventPublisher eventPublisher,
                                              DynamicActionExecutorRegistry actionExecutorRegistry,
                                              DynamicActionTransactionOperator actionTransactionOperator,
                                              ObjectProvider<FieldCryptoProvider> fieldCryptoProvider,
                                              ObjectProvider<FieldSigner> fieldSigner,
                                              PlatformTimeService platformTimeService,
                                              DatabaseValueConverter databaseValueConverter) {
        return DynamicRecordRuntime.builder(operations)
                .registry(new DynamicModuleRegistry())
                .fieldValueValidator(fieldValueValidator)
                .eventPublisher(eventPublisher)
                .actionExecutorRegistry(actionExecutorRegistry)
                .actionTransactionOperator(actionTransactionOperator)
                .fieldProtection(
                        fieldCryptoProvider.getIfAvailable(() -> FieldCryptoProvider.UNAVAILABLE),
                        fieldSigner.getIfAvailable(() -> FieldSigner.UNAVAILABLE))
                .timeService(platformTimeService)
                .valueConverter(databaseValueConverter)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicRecordService dynamicRecordService(DynamicRecordRuntime runtime,
                                              ObjectProvider<ActionExecutionPolicyService> actionExecutionPolicyService,
                                              ObjectProvider<DataScopeCriteriaService> dataScopeCriteriaService,
                                              ObjectProvider<DynamicRecordMutationCoordinator> mutationCoordinator) {
        return new DynamicRecordService(runtime,
                actionExecutionPolicyService.getIfAvailable(AllowAllActionExecutionPolicyService::new),
                dataScopeCriteriaService.getIfAvailable(AllowAllDataScopeCriteriaService::new),
                DynamicRecordMutationCoordinators.lazyComposite(() -> mutationCoordinator.orderedStream().toList()));
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicReferenceDependencyScopeResolver dynamicReferenceDependencyScopeResolver(DynamicRecordRuntime runtime) {
        return new DynamicReferenceDependencyScopeResolver(runtime);
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicActionExecutorRegistry dynamicActionExecutorRegistry(ObjectProvider<DynamicActionExecutor> executors) {
        return new DynamicActionExecutorRegistry(() -> executors.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicActionTransactionOperator dynamicActionTransactionOperator(
            ObjectProvider<PlatformTransactionManager> transactionManager) {
        PlatformTransactionManager manager = transactionManager.getIfAvailable();
        if (manager == null) {
            return DynamicActionTransactionOperator.none();
        }
        TransactionTemplate transactionTemplate = new TransactionTemplate(manager);
        return (context, action) -> transactionTemplate.execute(status -> action.get());
    }

    @Bean
    @ConditionalOnMissingBean
    RuntimeEventPublisher runtimeEventPublisher(ObjectProvider<RuntimeEventListener> listeners) {
        return new RuntimeEventMulticaster(() -> listeners.orderedStream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicModuleRuntimeRefresher dynamicModuleRuntimeRefresher(DynamicSchemaService schemaService,
                                                  DynamicRecordRuntime runtime) {
        return new DynamicModuleRuntimeRefresher(schemaService, runtime);
    }
}
