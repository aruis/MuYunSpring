package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StaticRecordReadProjectionServiceTest {
    @Test
    void shouldKeepResponseWhenStaticDefinitionIsMissing() {
        StaticRecordReadProjectionService service = new StaticRecordReadProjectionService(
                new StaticModuleDefinitionCatalog(List.of())
        );
        ProjectionEmployee record = new ProjectionEmployee();
        record.setId("emp-1");
        record.setEmployeeNo("E001");
        WebPageResponse<ProjectionEmployee> response = WebPageResponse.fromList(List.of(record));

        WebPageResponse<?> projected = service.projectDefaultList(
                "iam.employee",
                response,
                null
        );

        assertThat(projected).isSameAs(response);
        assertThat(projected.records()).hasSize(1);
        assertThat(projected.records().get(0)).isSameAs(record);
    }

    @Test
    void shouldProjectResponseByStaticResolvedListView() {
        StaticRecordReadProjectionService service = new StaticRecordReadProjectionService(
                new StaticModuleDefinitionCatalog(List.of(staticDefinition()))
        );
        ProjectionEmployee record = new ProjectionEmployee();
        record.setId("emp-1");
        record.setEmployeeNo("E001");
        record.setTitle("Alice");
        record.setMobile("13800000000");
        WebPageResponse<ProjectionEmployee> response = WebPageResponse.fromList(List.of(record));

        WebPageResponse<?> projected = service.projectDefaultList(
                "iam.employee",
                response,
                null
        );

        assertThat(projected).isNotSameAs(response);
        assertThat(projected.records()).hasSize(1);
        Map<?, ?> output = (Map<?, ?>) projected.records().get(0);
        assertThat(output.get("id")).isEqualTo("emp-1");
        assertThat(output.get("employeeNo")).isEqualTo("E001");
        assertThat(output.get("title")).isEqualTo("Alice");
        assertThat(output.containsKey("mobile")).isFalse();
        assertThat(projected.total()).isEqualTo(response.total());
        assertThat(projected.pageNum()).isEqualTo(response.pageNum());
        assertThat(projected.pageSize()).isEqualTo(response.pageSize());
    }

    private static StaticModuleDefinition staticDefinition() {
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
                                FieldDefinition.string("title", "职员姓名"),
                                FieldDefinition.string("mobile", "手机号")
                        )
                )),
                ModuleUiDefinition.builder("iam.employee")
                        .listView(list -> list
                                .field("employeeNo")
                                .field("title"))
                        .build()
        );
    }

    public static final class ProjectionEmployee {
        private String id;
        private String employeeNo;
        private String title;
        private String mobile;

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
    }
}
