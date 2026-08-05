package net.ximatai.muyun.spring.starter.configuration.platform;

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

/**
 * 选项与枚举装配：集中注册候选项来源和静态字段校验器，
 * 让静态模型与动态字段使用同一套选项语义。
 */
@Configuration(proxyBeanMethods = false)
public class MuYunSpringOptionConfiguration {
    @Bean
    @ConditionalOnMissingBean
    /** 将实现 CodeTitle 契约的枚举暴露为标准选项来源。 */
    CodeTitleEnumOptionSourceProvider codeTitleEnumOptionSourceProvider() {
        return new CodeTitleEnumOptionSourceProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    /** 按 Spring 排序聚合选项来源，保留领域继续贡献候选项的扩展点。 */
    OptionSourceRegistry optionSourceRegistry(List<OptionSourceProvider> providers) {
        return new OptionSourceRegistry(providers);
    }

    @Bean
    /** 将静态选项字段校验器桥接到 Ability 运行时，并在上下文销毁时复位。 */
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
