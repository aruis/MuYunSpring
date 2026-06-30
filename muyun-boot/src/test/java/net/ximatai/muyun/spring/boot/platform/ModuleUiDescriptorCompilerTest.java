package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleUiDescriptorCompilerTest {
    @Test
    void shouldCompileUiDefinitionToResolvedDescriptorWithoutPhysicalReadDetails() {
        ModuleUiDefinition definition = ModuleUiDefinition.builder("iam.employee")
                .listView(list -> list
                        .title("职员列表")
                        .field("employeeNo", field -> field.label("职员编号").width("160px"))
                        .field("enabled", field -> field.label("状态").uiType("enabledStatus").align("center")))
                .formView(form -> form
                        .title("职员档案")
                        .field("organizationId", field -> field.label("所属机构").required().readOnly())
                        .field("departmentId", field -> field.label("所属部门").required().uiType("recordPicker")))
                .build();

        ResolvedModuleUiDescriptor descriptor = ModuleUiDescriptorCompiler.compile(definition);

        assertThat(descriptor.moduleAlias()).isEqualTo("iam.employee");
        assertThat(descriptor.schemaVersion()).isEqualTo(ResolvedModuleUiDescriptor.SCHEMA_VERSION);
        assertThat(descriptor.views()).hasSize(2);
        assertThat(descriptor.views()).filteredOn(view -> view.viewCode().equals("default_list"))
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.viewKind()).isEqualTo(ModuleViewKind.LIST);
                    assertThat(view.fields()).extracting(field -> field.fieldRef().fieldName())
                            .containsExactly("employeeNo", "enabled");
                    assertThat(view.fields()).last()
                            .satisfies(field -> {
                                assertThat(field.uiType()).isEqualTo("enabledStatus");
                                assertThat(field.align()).isEqualTo("center");
                            });
                });
        assertThat(descriptor.views()).filteredOn(view -> view.viewCode().equals("default_form"))
                .singleElement()
                .satisfies(view -> {
                    assertThat(view.fields()).extracting(field -> field.fieldRef().fieldName())
                            .containsExactly("organizationId", "departmentId");
                    assertThat(view.fields()).first()
                            .satisfies(field -> {
                                assertThat(field.required().constant()).isTrue();
                                assertThat(field.readOnly().constant()).isTrue();
                            });
                    assertThat(view.fields()).last()
                            .satisfies(field -> assertThat(field.uiType()).isEqualTo("recordPicker"));
                });
    }

    @Test
    void shouldCompileStaticDefinitionWhenUiFieldsExistInModelFacts() {
        ModuleUiDefinition uiDefinition = ModuleUiDefinition.builder("iam.employee")
                .listView(list -> list
                        .field("employeeNo")
                        .field("title")
                        .field("enabled"))
                .build();

        ResolvedModuleUiDescriptor descriptor = ModuleUiDescriptorCompiler.compile(staticDefinition(uiDefinition));

        assertThat(descriptor.moduleKind()).isEqualTo(net.ximatai.muyun.spring.platform.module.ModuleKind.STATIC);
        assertThat(descriptor.title()).isEqualTo("职员管理");
        assertThat(descriptor.views()).singleElement()
                .satisfies(view -> assertThat(view.fields()).extracting(field -> field.fieldRef().fieldName())
                        .containsExactly("employeeNo", "title", "enabled"));
    }

    @Test
    void shouldCompileStaticDefinitionReadModelFromLogicalFieldFacts() {
        ModuleUiDefinition uiDefinition = ModuleUiDefinition.builder("iam.employee")
                .listView(list -> list
                        .field("employeeNo")
                        .field("title")
                        .field("enabled"))
                .build();

        ModuleUiCompilationResult result = ModuleUiDescriptorCompiler.compileModule(staticDefinition(uiDefinition));

        assertThat(result.readModel().moduleAlias()).isEqualTo("iam.employee");
        assertThat(result.readModel().mainEntityAlias()).isEqualTo("employee");
        assertThat(result.readModel().fields()).extracting(ResolvedModuleReadField::fieldName)
                .containsExactly("employeeNo", "mobile", "title", "enabled");
        assertThat(result.readModel().fields()).filteredOn(ResolvedModuleReadField::platformManaged)
                .extracting(ResolvedModuleReadField::fieldName)
                .containsExactly("title", "enabled");
    }

    @Test
    void shouldRejectStaticDefinitionFieldOutsideModelFacts() {
        ModuleUiDefinition uiDefinition = ModuleUiDefinition.builder("iam.employee")
                .listView(list -> list.field("employeeNo").field("ghostField"))
                .build();

        StaticModuleDefinition definition = staticDefinition(uiDefinition);

        assertThatThrownBy(() -> ModuleUiDescriptorCompiler.compile(definition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iam.employee.default_list.ghostField");
    }

    private StaticModuleDefinition staticDefinition(ModuleUiDefinition uiDefinition) {
        return new StaticModuleDefinition(
                "iam",
                "iam.employee",
                "职员管理",
                null,
                ModuleEntryType.ROUTE,
                "/iam/employees",
                null,
                Set.of(EntityCapability.CRUD),
                List.of(),
                List.of(new EntityDefinition(
                        "employee",
                        "iam_employee",
                        "Employee",
                        List.of(
                                FieldDefinition.string("employeeNo", "职员编号"),
                                FieldDefinition.string("mobile", "手机号")
                        )
                )),
                uiDefinition
        );
    }
}
