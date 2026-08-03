package net.ximatai.muyun.spring.platform.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.common.option.CodeTitleEnumOptionSourceProvider;
import net.ximatai.muyun.spring.common.option.DictionaryField;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OptionSourceStaticFieldValueValidatorTest {
    private final OptionSourceStaticFieldValueValidator validator =
            new OptionSourceStaticFieldValueValidator(new OptionSourceRegistry(List.of(
                    new GenderSourceProvider(),
                    new CodeTitleEnumOptionSourceProvider()
            )));

    @Test
    void shouldValidateSingleOptionFieldAgainstEnabledOptions() {
        Employee employee = new Employee();
        employee.gender = "1";

        validator.validate(Employee.class, employee);
    }

    @Test
    void shouldRejectDisabledOrMissingOptionCode() {
        Employee employee = new Employee();
        employee.gender = "0";

        assertThatThrownBy(() -> validator.validate(Employee.class, employee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid option code for field gender");
    }

    @Test
    void shouldRejectDuplicateMultipleOptionCode() {
        EmployeeTags employee = new EmployeeTags();
        employee.tags = List.of("1", "1");

        assertThatThrownBy(() -> validator.validate(EmployeeTags.class, employee))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate option code");
    }

    @Test
    void shouldValidateCodeTitleEnumOptionField() {
        RoleDraft draft = new RoleDraft();
        draft.kind = TestRoleKind.STANDARD;

        validator.validate(RoleDraft.class, draft);
    }

    @Test
    void shouldValidateCodeTitleEnumOptionCollectionField() {
        RoleKindsDraft draft = new RoleKindsDraft();
        draft.kinds = List.of(TestRoleKind.STANDARD, TestRoleKind.SYSTEM);

        validator.validate(RoleKindsDraft.class, draft);
    }

    @Test
    void shouldValidateStringCollectionFieldWithExplicitEnumType() {
        RoleKindCodesDraft draft = new RoleKindCodesDraft();
        draft.kinds = List.of("standard", "system");

        validator.validate(RoleKindCodesDraft.class, draft);
    }

    private static class Employee {
        @DictionaryField(source = "iam.gender")
        private String gender;

        private String genderTitle;
    }

    private static class EmployeeTags {
        @DictionaryField(source = "iam.gender",
                selectionMode = OptionSelectionMode.MULTIPLE)
        private List<String> tags;

        private List<String> tagsTitle;
    }

    private static class RoleDraft {
        @OptionField(type = OptionSourceType.ENUM)
        private TestRoleKind kind;

        private String kindTitle;
    }

    private static class RoleKindsDraft {
        @OptionField(type = OptionSourceType.ENUM, selectionMode = OptionSelectionMode.MULTIPLE)
        private List<TestRoleKind> kinds;

        private List<String> kindsTitle;
    }

    private static class RoleKindCodesDraft {
        @OptionField(type = OptionSourceType.ENUM,
                enumType = TestRoleKind.class,
                selectionMode = OptionSelectionMode.MULTIPLE)
        private List<String> kinds;

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
