package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.common.option.CodeTitleEnumOptionSourceProvider;
import net.ximatai.muyun.spring.common.option.DictionaryField;
import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionLoad;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSource;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OptionLoadProjectionPostProcessorTest {
    @Test
    void shouldResolveEnumConstantNameToCodeAndReplaceNullProjection() {
        RecordReadProjection projection = projection("parseStatusTitle");
        LinkedHashMap<String, Object> record = new LinkedHashMap<>();
        record.put("parseStatus", "PARSED");
        record.put("parseStatusTitle", null);

        List<Map<String, Object>> projected = OptionLoadProjectionPostProcessor.apply(
                EnumRecord.class,
                projection,
                List.of(record),
                new OptionSourceRegistry(List.of(new CodeTitleEnumOptionSourceProvider()))
        );

        assertThat(projected.getFirst()).containsEntry("parseStatus", "PARSED")
                .containsEntry("parseStatusTitle", "解析完成");
    }

    @Test
    void shouldKeepDictionaryCodeMatchingStrict() {
        RecordReadProjection projection = projection("statusTitle");
        LinkedHashMap<String, Object> record = new LinkedHashMap<>();
        record.put("status", "PARSED");
        record.put("statusTitle", null);

        List<Map<String, Object>> projected = OptionLoadProjectionPostProcessor.apply(
                DictionaryRecord.class,
                projection,
                List.of(record),
                new OptionSourceRegistry(List.of(new DictionarySourceProvider()))
        );

        assertThat(projected.getFirst()).containsEntry("status", "PARSED")
                .containsEntry("statusTitle", null);
    }

    private static RecordReadProjection projection(String outputField) {
        return new RecordReadProjection(
                "mr.knowledge_file",
                "default_list",
                List.of(ViewFieldRef.main(outputField)),
                List.of(),
                List.of(RecordReadPostTransform.optionLoad(outputField).serialize())
        );
    }

    private static class EnumRecord {
        @OptionField(type = OptionSourceType.ENUM, enumType = ParseStatus.class)
        private String parseStatus;

        @OptionLoad(source = "parseStatus")
        private String parseStatusTitle;
    }

    private static class DictionaryRecord {
        @DictionaryField(source = "mr.parse_status")
        private String status;

        @OptionLoad(source = "status")
        private String statusTitle;
    }

    private enum ParseStatus implements CodeTitleEnum {
        PARSED("parsed", "解析完成");

        private final String code;
        private final String title;

        ParseStatus(String code, String title) {
            this.code = code;
            this.title = title;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    private static class DictionarySourceProvider implements OptionSourceProvider {
        @Override
        public String sourceType() {
            return OptionBinding.DICTIONARY_SOURCE;
        }

        @Override
        public OptionSource source(OptionBinding binding) {
            return new OptionSource() {
                @Override
                public OptionBinding binding() {
                    return binding;
                }

                @Override
                public List<OptionItem> options(OptionQuery query) {
                    return List.of(new OptionItem("parsed", "解析完成", true, 1, null));
                }
            };
        }
    }
}
