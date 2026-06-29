package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.option.StaticOptionFieldValueValidator;
import net.ximatai.muyun.spring.common.option.CodeTitleEnumOptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
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

    @Bean
    StaticOptionFieldValueValidatorRegistration staticOptionFieldValueValidatorRegistration(
            ObjectProvider<StaticOptionFieldValueValidator> validatorProvider) {
        return new StaticOptionFieldValueValidatorRegistration(
                validatorProvider.getIfAvailable(() -> StaticOptionFieldValueValidator.NONE));
    }

    static final class StaticOptionFieldValueValidatorRegistration implements DisposableBean {
        StaticOptionFieldValueValidatorRegistration(StaticOptionFieldValueValidator validator) {
            PlatformAbilityRuntime.configureStaticOptionFieldValueValidator(validator);
        }

        @Override
        public void destroy() {
            PlatformAbilityRuntime.resetStaticOptionFieldValueValidator();
        }
    }
}
