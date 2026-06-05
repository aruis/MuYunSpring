package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
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
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutor;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionExecutorRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicActionTransactionOperator;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.publish.DynamicModulePublisher;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicReferenceDependencyScopeResolver;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.schema.DynamicSchemaService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryFieldValueValidator;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods = false)
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
    DynamicSchemaService dynamicSchemaService(IDatabaseOperations<?> operations) {
        return new DynamicSchemaService(operations);
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicRecordRuntime dynamicRecordRuntime(IDatabaseOperations<?> operations,
                                              DynamicFieldValueValidator fieldValueValidator,
                                              RuntimeEventPublisher eventPublisher,
                                              DynamicActionExecutorRegistry actionExecutorRegistry,
                                              DynamicActionTransactionOperator actionTransactionOperator,
                                              ObjectProvider<FieldCryptoProvider> fieldCryptoProvider,
                                              ObjectProvider<FieldSigner> fieldSigner) {
        return new DynamicRecordRuntime(operations, new DynamicModuleRegistry(), fieldValueValidator,
                eventPublisher, actionExecutorRegistry, actionTransactionOperator,
                fieldCryptoProvider.getIfAvailable(() -> FieldCryptoProvider.UNAVAILABLE),
                fieldSigner.getIfAvailable(() -> FieldSigner.UNAVAILABLE));
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicRecordService dynamicRecordService(DynamicRecordRuntime runtime,
                                              ObjectProvider<ActionExecutionPolicyService> actionExecutionPolicyService,
                                              ObjectProvider<DataScopeCriteriaService> dataScopeCriteriaService) {
        return new DynamicRecordService(runtime,
                actionExecutionPolicyService.getIfAvailable(AllowAllActionExecutionPolicyService::new),
                dataScopeCriteriaService.getIfAvailable(AllowAllDataScopeCriteriaService::new));
    }

    @Bean
    @ConditionalOnMissingBean
    ReferenceDependencyScopeResolver dynamicReferenceDependencyScopeResolver(DynamicRecordRuntime runtime) {
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
    DynamicModulePublisher dynamicModulePublisher(DynamicSchemaService schemaService,
                                                  DynamicRecordRuntime runtime) {
        return new DynamicModulePublisher(schemaService, runtime);
    }
}
