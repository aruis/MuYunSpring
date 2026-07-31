package net.ximatai.muyun.spring.boot.configuration.runtime;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 平台运行模式装配：将配置属性转换为 Schema 等治理能力可消费的运行态门面。 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MuYunSpringRuntimeProperties.class)
public class MuYunSpringRuntimeConfiguration {
    @Bean
    @ConditionalOnMissingBean
    /** 应用可覆盖此 Bean，以接入部署环境自己的运行模式决策。 */
    PlatformRuntimeModeProvider platformRuntimeModeProvider(MuYunSpringRuntimeProperties properties) {
        return new PropertiesPlatformRuntimeModeProvider(properties);
    }
}
