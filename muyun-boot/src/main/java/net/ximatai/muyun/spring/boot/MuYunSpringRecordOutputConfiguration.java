package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.option.StaticOptionFieldTitlePopulator;
import net.ximatai.muyun.spring.ability.output.DefaultPlatformRecordOutput;
import net.ximatai.muyun.spring.ability.output.FieldProtectionRecordOutputTransformer;
import net.ximatai.muyun.spring.ability.output.OptionTitleRecordOutputTransformer;
import net.ximatai.muyun.spring.ability.output.PlatformRecordOutput;
import net.ximatai.muyun.spring.ability.output.RecordOutputTransformer;
import net.ximatai.muyun.spring.web.WebOutputSupport;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class MuYunSpringRecordOutputConfiguration {
    @Bean
    @Order(0)
    RecordOutputTransformer optionTitleRecordOutputTransformer(
            ObjectProvider<StaticOptionFieldTitlePopulator> titlePopulatorProvider) {
        return new OptionTitleRecordOutputTransformer(
                titlePopulatorProvider.getIfAvailable(() -> StaticOptionFieldTitlePopulator.NONE)
        );
    }

    @Bean
    @Order(100)
    RecordOutputTransformer fieldProtectionRecordOutputTransformer() {
        return new FieldProtectionRecordOutputTransformer();
    }

    @Bean
    @ConditionalOnMissingBean
    PlatformRecordOutput platformRecordOutput(ObjectProvider<RecordOutputTransformer> transformerProvider) {
        List<RecordOutputTransformer> transformers = transformerProvider.orderedStream().toList();
        return new DefaultPlatformRecordOutput(transformers);
    }

    @Bean
    WebOutputSupportRegistration webOutputSupportRegistration(PlatformRecordOutput recordOutput) {
        return new WebOutputSupportRegistration(recordOutput);
    }

    static final class WebOutputSupportRegistration implements DisposableBean {
        WebOutputSupportRegistration(PlatformRecordOutput recordOutput) {
            WebOutputSupport.configure(recordOutput);
        }

        @Override
        public void destroy() {
            WebOutputSupport.reset();
        }
    }
}
