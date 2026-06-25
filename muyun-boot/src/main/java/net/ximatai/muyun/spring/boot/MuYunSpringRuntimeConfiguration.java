package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.runtime.PlatformRuntimeModeProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MuYunSpringRuntimeProperties.class)
public class MuYunSpringRuntimeConfiguration {
    @Bean
    @ConditionalOnMissingBean
    PlatformRuntimeModeProvider platformRuntimeModeProvider(MuYunSpringRuntimeProperties properties) {
        return new PropertiesPlatformRuntimeModeProvider(properties);
    }
}
