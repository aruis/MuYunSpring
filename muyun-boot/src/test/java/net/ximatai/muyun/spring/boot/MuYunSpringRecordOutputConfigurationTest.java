package net.ximatai.muyun.spring.boot;

import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.option.StaticOptionFieldTitlePopulator;
import net.ximatai.muyun.spring.ability.output.PlatformRecordOutput;
import net.ximatai.muyun.spring.ability.output.RecordOutputContext;
import net.ximatai.muyun.spring.ability.output.RecordOutputTransformer;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.common.di.ObjectProviders;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MuYunSpringRecordOutputConfigurationTest {
    private final MuYunSpringRecordOutputConfiguration configuration = new MuYunSpringRecordOutputConfiguration();

    @AfterEach
    void tearDown() {
        WebOutputSupport.reset();
    }

    @Test
    void shouldRegisterPlatformRecordOutputAndWireWebOutputSupportWithCustomTransformers() {
        RecordOutputTransformer optionTransformer = configuration.optionTitleRecordOutputTransformer(
                ObjectProviders.of(titlePopulator())
        );
        RecordOutputTransformer customTransformer = customRecordOutputTransformer();
        PlatformRecordOutput recordOutput = configuration.platformRecordOutput(
                ObjectProviders.of(List.of(optionTransformer, customTransformer))
        );

        try (MuYunSpringRecordOutputConfiguration.WebOutputSupportRegistration ignored =
                     configuration.webOutputSupportRegistration(recordOutput)) {
            OptionRecord record = new OptionRecord();
            record.setKind("standard");

            OptionRecord output = WebOutputSupport.record(new OptionRecordService(), record, FieldOutputContext.VIEW);

            assertThat(output).isSameAs(record);
            assertThat(output.getKindTitle()).isEqualTo("标准-custom");
        }
    }

    private StaticOptionFieldTitlePopulator titlePopulator() {
        return (modelClass, entity) -> {
            OptionRecord record = (OptionRecord) entity;
            if ("standard".equals(record.getKind())) {
                record.setKindTitle("标准");
            }
        };
    }

    private RecordOutputTransformer customRecordOutputTransformer() {
        return new RecordOutputTransformer() {
            @Override
            public <T extends EntityContract> T transformRecord(CrudAbility<T> service,
                                                                T record,
                                                                RecordOutputContext context) {
                if (record instanceof OptionRecord optionRecord && optionRecord.getKindTitle() != null) {
                    optionRecord.setKindTitle(optionRecord.getKindTitle() + "-custom");
                }
                return record;
            }
        };
    }

    private static final class OptionRecordService implements CrudAbility<OptionRecord> {
        @Override
        public BaseDao<OptionRecord, String> getDao() {
            throw new UnsupportedOperationException("dao is not used by record output");
        }

        @Override
        public String getModuleAlias() {
            return "test.option_record";
        }

        @Override
        public Class<?> modelClass() {
            return OptionRecord.class;
        }
    }

    private static final class OptionRecord extends StandardEntity {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "test.kind")
        private String kind;

        private String kindTitle;

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public String getKindTitle() {
            return kindTitle;
        }

        public void setKindTitle(String kindTitle) {
            this.kindTitle = kindTitle;
        }
    }
}
