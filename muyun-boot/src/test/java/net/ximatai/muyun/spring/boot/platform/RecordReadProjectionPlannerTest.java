package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordReadProjectionPlannerTest {
    @Test
    void shouldPlanDefaultListProjectionFromResolvedDescriptor() {
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(staticDefinition(
                ModuleUiDefinition.builder("iam.employee")
                        .listView(list -> list
                                .field("employeeNo")
                                .field("title")
                                .field("mobile", field -> field.visible(UiRule.constant(false)))
                                .field("enabled"))
                        .build()
        ));

        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        assertThat(projection.moduleAlias()).isEqualTo("iam.employee");
        assertThat(projection.viewCode()).isEqualTo("default_list");
        assertThat(projection.outputFields()).extracting(ViewFieldRef::fieldName)
                .containsExactly("employeeNo", "title", "enabled");
        assertThat(projection.requiredPlatformFields()).containsExactly("id", "tenantId", "version");
        assertThat(projection.readFields()).containsExactly("id", "tenantId", "version",
                "employeeNo", "title", "enabled");
        assertThat(projection.postReadTransforms()).isEmpty();
    }

    @Test
    void shouldRejectProjectionFieldOutsideReadModel() {
        ResolvedModuleUiDescriptor descriptor = new ResolvedModuleUiDescriptor(
                "iam.employee",
                List.of(new ResolvedViewDescriptor(
                        "default_list",
                        ModuleViewKind.LIST,
                        ModuleUiClientType.WEB,
                        null,
                        List.of(new ResolvedViewFieldDescriptor(
                                ViewFieldRef.main("ghostField"),
                                "Ghost",
                                UiRule.constant(true),
                                UiRule.constant(false),
                                UiRule.constant(false),
                                null,
                                null,
                                null,
                                null
                        ))
                ))
        );
        ResolvedModuleReadModel readModel = new ResolvedModuleReadModel(
                "iam.employee",
                "employee",
                List.of(new ResolvedModuleReadField("employee", null, "employeeNo", false))
        );

        assertThatThrownBy(() -> RecordReadProjectionPlanner.defaultList(descriptor, readModel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("iam.employee.default_list.ghostField");
    }

    @Test
    void shouldProjectRecordOutputByProjectionFields() {
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(staticDefinition(
                ModuleUiDefinition.builder("iam.employee")
                        .listView(list -> list
                                .field("employeeNo")
                                .field("title")
                                .field("enabled"))
                        .build()
        ));
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );
        ProjectionEmployee record = new ProjectionEmployee();
        record.setId("emp-1");
        record.setEmployeeNo("E001");
        record.setTitle("张三");
        record.setMobile("13800000000");
        record.setEnabled(Boolean.TRUE);

        Map<String, Object> output = RecordReadProjectionProjector.project(record, projection);

        assertThat(output).containsEntry("id", "emp-1");
        assertThat(output).containsEntry("employeeNo", "E001");
        assertThat(output).containsEntry("title", "张三");
        assertThat(output).containsEntry("enabled", Boolean.TRUE);
        assertThat(output).doesNotContainKey("mobile");
    }

    @Test
    void shouldKeepNullValuesWhenProjectingRecordOutput() {
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(staticDefinition(
                ModuleUiDefinition.builder("iam.employee")
                        .listView(list -> list.field("employeeNo"))
                        .build()
        ));
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );
        ProjectionEmployee record = new ProjectionEmployee();
        record.setId("emp-1");

        Map<String, Object> output = RecordReadProjectionProjector.project(record, projection);

        assertThat(output).containsEntry("id", "emp-1");
        assertThat(output).containsEntry("employeeNo", null);
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

    public static final class ProjectionEmployee {
        private String id;
        private String employeeNo;
        private String title;
        private String mobile;
        private Boolean enabled;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getEmployeeNo() {
            return employeeNo;
        }

        public void setEmployeeNo(String employeeNo) {
            this.employeeNo = employeeNo;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }
    }
}
