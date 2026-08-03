package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.output.DefaultPlatformRecordOutput;
import net.ximatai.muyun.spring.ability.output.OptionLoadRecordOutputTransformer;
import net.ximatai.muyun.spring.ability.output.PlatformRecordOutput;
import net.ximatai.muyun.spring.ability.output.RecordOutputContext;
import net.ximatai.muyun.spring.ability.output.RecordOutputTransformer;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.option.DictionaryField;
import net.ximatai.muyun.spring.common.option.OptionLoad;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformRecordOutputTest {
    @Test
    void shouldPopulateStaticOptionTitlesWithoutServiceSpecificOutputAbility() {
        PlatformRecordOutput output = new DefaultPlatformRecordOutput(List.of(
                new OptionLoadRecordOutputTransformer((modelClass, entity) -> {
                    OptionRecord record = (OptionRecord) entity;
                    if ("1".equals(record.getKind())) {
                        record.setKindTitle("标准");
                    }
                })
        ));
        OptionRecordService service = new OptionRecordService();
        OptionRecord record = new OptionRecord();
        record.setKind("1");

        OptionRecord transformed = output.record(service, record, RecordOutputContext.view());

        assertThat(transformed).isSameAs(record);
        assertThat(transformed.getKindTitle()).isEqualTo("标准");
    }

    @Test
    void shouldApplyRecordTransformersInOrderForLists() {
        PlatformRecordOutput output = new DefaultPlatformRecordOutput(List.of(
                new OptionLoadRecordOutputTransformer((modelClass, entity) ->
                        ((OptionRecord) entity).setKindTitle("标准")),
                new RecordOutputTransformer() {
                    @Override
                    public <T extends net.ximatai.muyun.spring.common.model.contract.EntityContract> List<T> transformRecords(
                            CrudAbility<T> service,
                            List<T> records,
                            RecordOutputContext context) {
                        records.forEach(record -> ((OptionRecord) record).setKindTitle(
                                ((OptionRecord) record).getKindTitle() + "-输出"
                        ));
                        return records;
                    }
                }
        ));
        OptionRecordService service = new OptionRecordService();
        OptionRecord record = new OptionRecord();
        record.setKind("1");

        List<OptionRecord> transformed = output.records(service, List.of(record), RecordOutputContext.list());

        assertThat(transformed).containsExactly(record);
        assertThat(record.getKindTitle()).isEqualTo("标准-输出");
    }

    private static final class OptionRecordService extends AbstractAbilityService<OptionRecord> {
        private OptionRecordService() {
            super("demo.optionRecord", OptionRecord.class, new InMemoryBaseDao<>());
        }
    }

    private static final class OptionRecord extends StandardEntity {
        @DictionaryField(source = "demo.kind")
        private String kind;

        @OptionLoad(source = "kind")
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
