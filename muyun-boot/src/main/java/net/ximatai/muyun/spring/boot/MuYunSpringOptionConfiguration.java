package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.PlatformAbilityRuntime;
import net.ximatai.muyun.spring.ability.option.StaticOptionFieldValueValidator;
import net.ximatai.muyun.spring.common.option.CodeTitleEnumOptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import net.ximatai.muyun.spring.common.di.ObjectProvider;

import java.util.List;

@ApplicationScoped
public class MuYunSpringOptionConfiguration {
    @Produces
    @ApplicationScoped
    @DefaultBean
    CodeTitleEnumOptionSourceProvider codeTitleEnumOptionSourceProvider() {
        return new CodeTitleEnumOptionSourceProvider();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    OptionSourceRegistry optionSourceRegistry(List<OptionSourceProvider> providers) {
        return new OptionSourceRegistry(providers);
    }

    @Produces
    @ApplicationScoped
    StaticOptionFieldValueValidatorRegistration staticOptionFieldValueValidatorRegistration(
            ObjectProvider<StaticOptionFieldValueValidator> validatorProvider) {
        return new StaticOptionFieldValueValidatorRegistration(
                validatorProvider.getIfAvailable(() -> StaticOptionFieldValueValidator.NONE));
    }

    static final class StaticOptionFieldValueValidatorRegistration implements AutoCloseable {
        StaticOptionFieldValueValidatorRegistration(StaticOptionFieldValueValidator validator) {
            PlatformAbilityRuntime.configureStaticOptionFieldValueValidator(validator);
        }

        @Override
        public void close() {
            PlatformAbilityRuntime.resetStaticOptionFieldValueValidator();
        }
    }
}
