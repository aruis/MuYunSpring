package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.event.ModuleExtension;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerListener;
import net.ximatai.muyun.spring.ability.event.RuntimeEventHandlerRegistry;
import net.ximatai.muyun.spring.ability.event.RuntimeEventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration(proxyBeanMethods = false)
public class MuYunSpringRuntimeEventHandlerConfiguration {
    @Bean
    @ConditionalOnMissingBean
    RuntimeEventHandlerRegistry runtimeEventHandlerRegistry(ApplicationContext applicationContext) {
        Map<String, Object> beans = new LinkedHashMap<>(
                applicationContext.getBeansWithAnnotation(ModuleExtension.class)
        );
        return RuntimeEventHandlerRegistry.fromBeans(
                beans,
                AopUtils::getTargetClass,
                (bean, method) -> AopUtils.selectInvocableMethod(method, bean.getClass())
        );
    }

    @Bean
    @ConditionalOnMissingBean(name = "runtimeEventHandlerListener")
    RuntimeEventListener runtimeEventHandlerListener(RuntimeEventHandlerRegistry registry) {
        return new RuntimeEventHandlerListener(registry);
    }
}
