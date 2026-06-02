package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.common.option.CodeTitleEnumOptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class MuYunSpringOptionConfiguration {
    @Bean
    @ConditionalOnMissingBean
    CodeTitleEnumOptionSourceProvider codeTitleEnumOptionSourceProvider() {
        return new CodeTitleEnumOptionSourceProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    OptionSourceRegistry optionSourceRegistry(List<OptionSourceProvider> providers) {
        return new OptionSourceRegistry(providers);
    }
}
