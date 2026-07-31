package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.option.OptionField;
import net.ximatai.muyun.spring.common.option.OptionSourceType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.title.TitleField;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoad;
import net.ximatai.muyun.spring.ability.reference.ReferenceHop;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
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
    void shouldPublishRecordLabelFactWithoutUiContributor() {
        StaticModuleDefinition definition = StaticModuleDefinition.builder("demo", "demo.customer", "客户管理")
                .entities(List.of(new EntityDefinition("customer", "demo_customer", "Customer",
                        List.of(FieldDefinition.string("displayName", "显示名称")))))
                .modelClass(CustomerRecord.class)
                .build();

        ModuleUiCompilationResult result = ModuleUiDescriptorCompiler.compileModule(definition);

        assertThat(result.uiDescriptor().recordLabelField()).isEqualTo("displayName");
        assertThat(result.uiDescriptor().views()).isEmpty();
        assertThat(result.uiDescriptor().actions()).isEmpty();
    }

    @Test
    void shouldCompileStaticOptionFieldAsResolvedFieldFact() {
        ModuleUiDefinition uiDefinition = ModuleUiDefinition.builder("iam.employee")
                .formView(form -> form.field("gender", field -> field.label("性别")))
                .build();
        StaticModuleDefinition definition = StaticModuleDefinition.builder("iam", "iam.employee", "职员管理")
                .entities(List.of(new EntityDefinition("employee", "iam_employee", "Employee",
                        List.of(FieldDefinition.string("gender", "性别")))))
                .uiDefinition(uiDefinition)
                .modelClass(OptionEmployee.class)
                .build();

        ResolvedViewFieldDescriptor field = ModuleUiDescriptorCompiler.compile(definition).views().getFirst()
                .fields().getFirst();

        assertThat(field.option()).isNotNull();
        assertThat(field.option().binding().sourceType()).isEqualTo("dictionary");
        assertThat(field.option().binding().source()).isEqualTo("iam.gender");
        assertThat(field.option().selectionMode().name()).isEqualTo("SINGLE");
        assertThat(field.option().titleField()).isEqualTo("genderTitle");
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

    @Test
    void shouldPublishReferenceOutputsAsStaticReadModelFields() {
        ModuleUiDefinition uiDefinition = ModuleUiDefinition.builder("sales.order")
                .listView(list -> list.field("orderNo").field("customerTitle").field("customerLevel"))
                .build();
        StaticModuleDefinition definition = StaticModuleDefinition.builder("sales", "sales.order", "订单")
                .entities(List.of(new EntityDefinition("order", "sales_order", "Order",
                        List.of(FieldDefinition.string("orderNo", "订单号"),
                                FieldDefinition.string("customerId", "客户")))))
                .uiDefinition(uiDefinition)
                .modelClass(ReferenceOrder.class)
                .build();

        ModuleUiCompilationResult result = ModuleUiDescriptorCompiler.compileModule(definition);

        assertThat(result.readModel().fields()).extracting(ResolvedModuleReadField::fieldName)
                .containsExactly("orderNo", "customerId", "customerTitle", "customerLevel");
        assertThat(result.readModel().fields()).filteredOn(ResolvedModuleReadField::platformManaged)
                .extracting(ResolvedModuleReadField::fieldName)
                .containsExactly("customerTitle", "customerLevel");
    }

    @Test
    void shouldPublishMultiHopReferenceLoadAsStaticReadModelField() {
        ModuleUiDefinition uiDefinition = ModuleUiDefinition.builder("sales.order")
                .listView(list -> list.field("orderNo").field("assistantTitle"))
                .build();
        StaticModuleDefinition definition = StaticModuleDefinition.builder("sales", "sales.order", "订单")
                .entities(List.of(new EntityDefinition("order", "sales_order", "Order",
                        List.of(FieldDefinition.string("orderNo", "订单号"),
                                FieldDefinition.string("classroomId", "班级")))))
                .uiDefinition(uiDefinition)
                .modelClass(MultiHopReferenceOrder.class)
                .build();

        ModuleUiCompilationResult result = ModuleUiDescriptorCompiler.compileModule(definition);

        assertThat(result.readModel().fields()).extracting(ResolvedModuleReadField::fieldName)
                .containsExactly("orderNo", "classroomId", "assistantTitle");
    }

    @Test
    void shouldRejectChildResourceFormWhenRelationIsOutsideModelFacts() {
        ModuleUiDefinition uiDefinition = ModuleUiDefinition.builder("iam.position_category")
                .formView(ModuleUiViewCodes.childResourceDefaultForm("position"), form -> form
                        .field("position", "code", field -> field.label("岗位编码"))
                        .field("ghost", "code", field -> field.label("错误资源")))
                .build();

        StaticModuleDefinition definition = staticDefinition(uiDefinition, positionEntities());

        assertThatThrownBy(() -> ModuleUiDescriptorCompiler.compile(definition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("static module UI relation is not declared by model facts")
                .hasMessageContaining("iam.position_category.position_default_form.ghost");
    }

    @Test
    void shouldRejectChildResourceFormWhenFieldIsOutsideRelationModelFacts() {
        ModuleUiDefinition uiDefinition = ModuleUiDefinition.builder("iam.position_category")
                .formView(ModuleUiViewCodes.childResourceDefaultForm("position"), form -> form
                        .field("position", "ghostField", field -> field.label("错误字段")))
                .build();

        StaticModuleDefinition definition = staticDefinition(uiDefinition, positionEntities());

        assertThatThrownBy(() -> ModuleUiDescriptorCompiler.compile(definition))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("static module UI field is not declared by model facts")
                .hasMessageContaining("iam.position_category.position_default_form.position.ghostField");
    }

    private StaticModuleDefinition staticDefinition(ModuleUiDefinition uiDefinition) {
        return staticDefinition(uiDefinition, employeeEntities());
    }

    private StaticModuleDefinition staticDefinition(ModuleUiDefinition uiDefinition, List<EntityDefinition> entities) {
        return StaticModuleDefinition.builder("iam", uiDefinition.moduleAlias(), "职员管理")
                       .parentModuleAlias(null)
                       .entry(ModuleEntryType.ROUTE, "/iam/employees", null)
                       .capabilities(Set.of(EntityCapability.CRUD))
                       .actions(List.of())
                       .entities(entities)
                       .uiDefinition(uiDefinition)
                       .build();
    }

    private List<EntityDefinition> employeeEntities() {
        return List.of(new EntityDefinition(
                        "employee",
                        "iam_employee",
                        "Employee",
                        List.of(
                                FieldDefinition.string("employeeNo", "职员编号"),
                                FieldDefinition.string("mobile", "手机号")
                        )
                )
        );
    }

    private List<EntityDefinition> positionEntities() {
        return List.of(
                new EntityDefinition(
                        "position_category",
                        "iam_position_category",
                        "PositionCategory",
                        List.of(FieldDefinition.string("code", "分类编码"))
                ),
                new EntityDefinition(
                        "position",
                        "iam_position",
                        "Position",
                        List.of(
                                FieldDefinition.string("categoryId", "所属分类"),
                                FieldDefinition.string("code", "岗位编码")
                        )
                )
        );
    }

    private static class OptionEmployee {
        @OptionField(type = OptionSourceType.DICTIONARY, source = "iam.gender")
        private String gender;

        private String genderTitle;
    }

    private static final class CustomerRecord extends StandardEntity {
        @TitleField
        private String displayName;
    }

    private static final class ReferenceOrder {
        @ReferenceTo(moduleAlias = "crm", entityAlias = "customer")
        private String customerId;

        @ReferenceLoad(source = "customerId", field = "title")
        private transient String customerTitle;

        @ReferenceLoad(source = "customerId", field = "level")
        private transient String customerLevel;
    }

    private static final class MultiHopReferenceOrder {
        @ReferenceTo(moduleAlias = "education.school", entityAlias = "classroom")
        private String classroomId;

        @ReferenceLoad(source = "classroomId", hops = @ReferenceHop(target = AssistantService.class, via = "assistantId"))
        private transient String assistantTitle;
    }

    public static final class AssistantService {
        public static final String MODULE_ALIAS = "education.school.assistant";
    }
}
