package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.option.StaticOptionFieldTitlePopulator;
import net.ximatai.muyun.spring.ability.output.DefaultPlatformRecordOutput;
import net.ximatai.muyun.spring.ability.output.FieldProtectionRecordOutputTransformer;
import net.ximatai.muyun.spring.ability.output.OptionTitleRecordOutputTransformer;
import net.ximatai.muyun.spring.ability.output.PlatformRecordOutput;
import net.ximatai.muyun.spring.ability.output.RecordOutputTransformer;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import io.quarkus.arc.DefaultBean;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import net.ximatai.muyun.spring.common.di.ObjectProvider;

import java.util.List;

@ApplicationScoped
public class MuYunSpringRecordOutputConfiguration {
    @Produces
    @ApplicationScoped
    @Priority(0)
    RecordOutputTransformer optionTitleRecordOutputTransformer(
            ObjectProvider<StaticOptionFieldTitlePopulator> titlePopulatorProvider) {
        return new OptionTitleRecordOutputTransformer(
                titlePopulatorProvider.getIfAvailable(() -> StaticOptionFieldTitlePopulator.NONE)
        );
    }

    @Produces
    @ApplicationScoped
    @Priority(100)
    RecordOutputTransformer fieldProtectionRecordOutputTransformer() {
        return new FieldProtectionRecordOutputTransformer();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    PlatformRecordOutput platformRecordOutput(ObjectProvider<RecordOutputTransformer> transformerProvider) {
        List<RecordOutputTransformer> transformers = transformerProvider.orderedStream().toList();
        return new DefaultPlatformRecordOutput(transformers);
    }

    @Produces
    @ApplicationScoped
    WebOutputSupportRegistration webOutputSupportRegistration(PlatformRecordOutput recordOutput) {
        return new WebOutputSupportRegistration(recordOutput);
    }

    static final class WebOutputSupportRegistration implements AutoCloseable {
        WebOutputSupportRegistration(PlatformRecordOutput recordOutput) {
            WebOutputSupport.configure(recordOutput);
        }

        @Override
        public void close() {
            WebOutputSupport.reset();
        }
    }
}
