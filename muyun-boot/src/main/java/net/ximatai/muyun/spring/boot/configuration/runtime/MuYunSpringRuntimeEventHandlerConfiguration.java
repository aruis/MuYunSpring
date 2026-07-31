package net.ximatai.muyun.spring.boot.configuration.runtime;

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

/**
 * 模块运行事件装配：发现 {@link ModuleExtension} 并注册可调用处理器，
 * 让领域扩展通过能力事件接入，而不是由 Web 或启动入口硬编码分发。
 */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringRuntimeEventHandlerConfiguration {
    @Bean
    @ConditionalOnMissingBean
    /** 使用目标类而不是代理类扫描扩展方法，避免 AOP 改变事件发现结果。 */
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
    /** 将已发现的处理器接到统一事件多播链路。 */
    RuntimeEventListener runtimeEventHandlerListener(RuntimeEventHandlerRegistry registry) {
        return new RuntimeEventHandlerListener(registry);
    }
}
