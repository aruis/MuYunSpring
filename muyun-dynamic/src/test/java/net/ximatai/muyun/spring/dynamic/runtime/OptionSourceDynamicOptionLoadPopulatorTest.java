package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSource;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OptionSourceDynamicOptionLoadPopulatorTest {
    @Test
    void shouldPopulateVirtualFieldsFromDictionaryItems() {
        EntityDefinition entity = new EntityDefinition("teacher", "edu_teacher", "教师", List.of(
                FieldDefinition.string("subjectCode", "学科").column("subject_code")
                        .dictionary("education", "teaching_subject"),
                FieldDefinition.string("subjectTitle", "学科名称").column("subject_title")
                        .virtual().optionLoad("subjectCode"),
                FieldDefinition.bool("subjectEnabled", "学科启用").column("subject_enabled")
                        .virtual().optionLoad("subjectCode", "enabled")
        ));
        DynamicRecord record = new DynamicRecord(entity).setValue("subjectCode", "math");
        OptionBinding binding = OptionBinding.dictionary("education", "teaching_subject");
        OptionSource source = new OptionSource() {
            @Override
            public OptionBinding binding() {
                return binding;
            }

            @Override
            public List<OptionItem> options(OptionQuery query) {
                return List.of(new OptionItem("math", "数学", true, 1, null));
            }
        };
        OptionSourceRegistry registry = new OptionSourceRegistry(List.of(new OptionSourceProvider() {
            @Override
            public String sourceType() {
                return OptionBinding.DICTIONARY_SOURCE;
            }

            @Override
            public OptionSource source(OptionBinding requested) {
                return source;
            }
        }));

        new OptionSourceDynamicOptionLoadPopulator(registry).populate(entity, List.of(record));

        assertThat(record.getValue("subjectTitle")).isEqualTo("数学");
        assertThat(record.getValue("subjectEnabled")).isEqualTo(true);
    }
}
