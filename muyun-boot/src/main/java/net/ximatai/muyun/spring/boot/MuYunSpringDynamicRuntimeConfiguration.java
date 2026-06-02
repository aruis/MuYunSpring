package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.spring.ability.event.RuntimeEventPublisher;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import net.ximatai.muyun.spring.dynamic.publish.DynamicModulePublisher;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicModuleRegistry;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordRuntime;
import net.ximatai.muyun.spring.dynamic.schema.DynamicSchemaService;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryFieldValueValidator;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItemService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                                              RuntimeEventPublisher eventPublisher) {
        return new DynamicRecordRuntime(operations, new DynamicModuleRegistry(), fieldValueValidator, eventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    RuntimeEventPublisher runtimeEventPublisher() {
        return RuntimeEventPublisher.noop();
    }

    @Bean
    @ConditionalOnMissingBean
    DynamicModulePublisher dynamicModulePublisher(DynamicSchemaService schemaService,
                                                  DynamicRecordRuntime runtime) {
        return new DynamicModulePublisher(schemaService, runtime);
    }
}
