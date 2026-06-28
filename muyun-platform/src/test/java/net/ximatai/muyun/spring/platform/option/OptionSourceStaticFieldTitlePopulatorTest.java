package net.ximatai.muyun.spring.platform.option;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.option.OptionSource;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OptionSourceStaticFieldTitlePopulatorTest {
    private final OptionSourceStaticFieldTitlePopulator populator =
            new OptionSourceStaticFieldTitlePopulator(new OptionSourceRegistry(List.of(new GenderSourceProvider())));

    @Test
    void shouldPopulateAutoTitleOutputField() {
        Employee employee = new Employee();
        employee.gender = "1";

        populator.populate(Employee.class, employee);

        assertThat(employee.genderTitle).isEqualTo("男");
    }

    @Test
    void shouldUseAllOptionsForTitleOutput() {
        Employee employee = new Employee();
        employee.gender = "0";

        populator.populate(Employee.class, employee);

        assertThat(employee.genderTitle).isEqualTo("停用");
    }

    @Test
    void shouldPopulateMultipleTitleOutputField() {
        EmployeeTags employee = new EmployeeTags();
        employee.tags = List.of("1", "0", "missing");

        populator.populate(EmployeeTags.class, employee);

        assertThat(employee.tagsTitle).containsExactly("男", "停用");
    }

    @Test
    void shouldNotFailOutputWhenOptionSourceIsMissing() {
        OptionSourceStaticFieldTitlePopulator missingSourcePopulator =
                new OptionSourceStaticFieldTitlePopulator(new OptionSourceRegistry(List.of(new MissingSourceProvider())));
        Employee employee = new Employee();
        employee.gender = "1";
        employee.genderTitle = "old";

        missingSourcePopulator.populate(Employee.class, employee);

        assertThat(employee.genderTitle).isNull();
    }

    private static class Employee {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "iam.gender")
        private String gender;

        private String genderTitle;
    }

    private static class EmployeeTags {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "iam.gender",
                selectionMode = OptionSelectionMode.MULTIPLE)
        private List<String> tags;

        private List<String> tagsTitle;
    }

    private static class GenderSourceProvider implements OptionSourceProvider {
        @Override
        public String sourceType() {
            return OptionBinding.DICTIONARY_SOURCE;
        }

        @Override
        public boolean supports(OptionBinding binding) {
            return OptionBinding.dictionary("iam", "gender").equals(binding);
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
                    List<OptionItem> options = List.of(
                            new OptionItem("1", "男", true, 1, null),
                            new OptionItem("0", "停用", false, 2, null)
                    );
                    if (query != null && query.onlyEnabled()) {
                        return options.stream().filter(OptionItem::enabled).toList();
                    }
                    return options;
                }
            };
        }
    }

    private static class MissingSourceProvider implements OptionSourceProvider {
        @Override
        public String sourceType() {
            return OptionBinding.DICTIONARY_SOURCE;
        }

        @Override
        public boolean supports(OptionBinding binding) {
            return true;
        }

        @Override
        public OptionSource source(OptionBinding binding) {
            throw new IllegalArgumentException("missing source");
        }
    }
}
