package net.ximatai.muyun.spring.platform.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.common.option.CodeTitleEnumOptionSourceProvider;
import net.ximatai.muyun.spring.common.option.DictionaryField;
import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionLoad;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.option.OptionSource;
import net.ximatai.muyun.spring.common.option.OptionSourceProvider;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionSourceStaticOptionLoadPopulatorTest {
    private final OptionSourceStaticOptionLoadPopulator populator =
            new OptionSourceStaticOptionLoadPopulator(new OptionSourceRegistry(List.of(
                    new GenderSourceProvider(),
                    new CodeTitleEnumOptionSourceProvider()
            )));

    @Test
    void shouldPopulateDictionaryTitleLoad() {
        Employee employee = new Employee();
        employee.gender = "1";

        populator.populate(Employee.class, employee);

        assertThat(employee.genderTitle).isEqualTo("男");
    }

    @Test
    void shouldUseDisabledDictionaryItemForLoad() {
        Employee employee = new Employee();
        employee.gender = "0";

        populator.populate(Employee.class, employee);

        assertThat(employee.genderTitle).isEqualTo("停用");
        assertThat(employee.genderEnabled).isFalse();
    }

    @Test
    void shouldPopulateMultipleOptionLoad() {
        EmployeeTags employee = new EmployeeTags();
        employee.tags = List.of("1", "0", "missing");

        populator.populate(EmployeeTags.class, employee);

        assertThat(employee.tagsTitle).containsExactly("男", "停用");
    }

    @Test
    void shouldExposeConfigurationErrorWhenOptionSourceIsMissing() {
        OptionSourceStaticOptionLoadPopulator missingSourcePopulator =
                new OptionSourceStaticOptionLoadPopulator(new OptionSourceRegistry(List.of(new MissingSourceProvider())));
        Employee employee = new Employee();
        employee.gender = "1";
        employee.genderTitle = "old";

        assertThatThrownBy(() -> missingSourcePopulator.populate(Employee.class, employee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing source");

        assertThat(employee.genderTitle).isEqualTo("old");
    }

    @Test
    void shouldPopulateStaticEnumOptionLoad() {
        RoleDraft draft = new RoleDraft();
        draft.kind = TestRoleKind.STANDARD;

        populator.populate(RoleDraft.class, draft);

        assertThat(draft.kindTitle).isEqualTo("标准角色");
    }

    @Test
    void shouldPopulateStaticEnumCollectionOptionLoad() {
        RoleKindsDraft draft = new RoleKindsDraft();
        draft.kinds = List.of(TestRoleKind.STANDARD, TestRoleKind.SYSTEM);

        populator.populate(RoleKindsDraft.class, draft);

        assertThat(draft.kindsTitle).containsExactly("标准角色", "系统角色");
    }

    private static class Employee {
        @DictionaryField(source = "iam.gender")
        private String gender;

        @OptionLoad(source = "gender")
        private String genderTitle;

        @OptionLoad(source = "gender", field = "enabled")
        private Boolean genderEnabled;
    }

    private static class EmployeeTags {
        @DictionaryField(source = "iam.gender",
                selectionMode = OptionSelectionMode.MULTIPLE)
        private List<String> tags;

        @OptionLoad(source = "tags")
        private List<String> tagsTitle;
    }

    private static class RoleDraft {
        @OptionField(type = OptionSourceType.ENUM)
        private TestRoleKind kind;

        @OptionLoad(source = "kind")
        private String kindTitle;
    }

    private static class RoleKindsDraft {
        @OptionField(type = OptionSourceType.ENUM, selectionMode = OptionSelectionMode.MULTIPLE)
        private List<TestRoleKind> kinds;

        @OptionLoad(source = "kinds")
        private List<String> kindsTitle;
    }

    private static class GenderSourceProvider implements OptionSourceProvider {
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
        public OptionSource source(OptionBinding binding) {
            throw new IllegalArgumentException("missing source");
        }
    }

    private enum TestRoleKind implements CodeTitleEnum {
        STANDARD("standard", "标准角色"),
        SYSTEM("system", "系统角色");

        private final String code;
        private final String title;

        TestRoleKind(String code, String title) {
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
}
