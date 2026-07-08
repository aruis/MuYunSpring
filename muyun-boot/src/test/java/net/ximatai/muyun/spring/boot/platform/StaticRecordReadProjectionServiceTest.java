package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldExecuteRelationProjectionSqlWithNestedCriteriaFieldsAndResponseFieldBoundary() {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        StaticRecordReadProjectionService service = new StaticRecordReadProjectionService(
                new StaticModuleDefinitionCatalog(List.of(userRelationDefinition())),
                new RelationProjectionQueryExecutor(jdbcOperations),
                new RelationProjectionDatabaseTypeProvider()
        );
        Criteria criteria = Criteria.of().eq("tenantId", "tenant_a")
                .andGroup(group -> group
                        .eq("passwordStatus", "ACTIVE")
                        .andGroup(nested -> nested.eq("createdAt", java.time.Instant.EPOCH)));
        when(jdbcOperations.queryForList(any(String.class), any(Map.class)))
                .thenReturn(List.of(Map.of(
                        "id", "user-1",
                        "tenantId", "tenant_a",
                        "version", 1,
                        "username", "alice",
                        "employeeNo", "E001",
                        "employeeTitle", "Alice"
                )));
        when(jdbcOperations.queryForObject(any(String.class), any(Map.class), eq(Long.class)))
                .thenReturn(1L);

        WebPageResponse<?> response = service.queryDefaultList(
                "iam.user",
                criteria,
                PageRequest.of(1, 20),
                null,
                Sort.desc("lastLoginAt")
        ).orElseThrow();

        assertThat(response.total()).isEqualTo(1);
        @SuppressWarnings("unchecked")
        Map<String, Object> output = (Map<String, Object>) response.records().getFirst();
        assertThat(output).containsEntry("employeeNo", "E001");
        assertThat(output).doesNotContainKeys("passwordStatus", "createdAt", "lastLoginAt", "deleted");
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.captor();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(jdbcOperations).queryForList(sqlCaptor.capture(), paramsCaptor.capture());
        String dataSql = sqlCaptor.getValue();
        assertThat(dataSql).startsWith("select ");
        assertThat(dataSql).contains("\"id\"", "\"username\"",
                "\"employeeNo\"", "\"employeeTitle\"", " from (");
        assertThat(dataSql.substring(0, dataSql.indexOf(" from (")))
                .doesNotContain("\"tenantId\"", "\"version\"", "\"deleted\"");
        assertThat(dataSql).contains("\"main\".\"password_status\" as \"passwordStatus\"");
        assertThat(dataSql).contains("\"main\".\"created_at\" as \"createdAt\"");
        assertThat(dataSql).contains("\"main\".\"last_login_at\" as \"lastLoginAt\"");
        assertThat(dataSql).contains("left join \"public\".\"iam_employee_account\" \"bound_employee_account\"");
        assertThat(dataSql).contains("left join \"public\".\"iam_employee\" \"bound_employee\"");
        assertThat(dataSql).contains("order by \"lastLoginAt\" desc");
        assertThat(paramsCaptor.getValue()).containsKeys("__limit", "__offset",
                "__join_bound_employee_bound_employee_account_0",
                "__join_bound_employee_bound_employee_0");
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

    private static StaticModuleDefinition userRelationDefinition() {
        return new StaticModuleDefinition(
                "iam",
                "iam.user",
                "用户管理",
                null,
                ModuleEntryType.ROUTE,
                "/iam/users",
                null,
                Set.of(EntityCapability.CRUD),
                List.of(),
                List.of(
                        new EntityDefinition(
                                "user",
                                "iam_user",
                                "User",
                                List.of(
                                        FieldDefinition.string("username", "账号").column("username"),
                                        FieldDefinition.string("passwordStatus", "密码状态").column("password_status"),
                                        FieldDefinition.timestamp("lastLoginAt", "最后登录时间").column("last_login_at")
                                )
                        ),
                        new EntityDefinition(
                                "bound_employee",
                                "iam_employee",
                                "绑定职员",
                                List.of(
                                        FieldDefinition.string("employeeNo", "职员工号").column("employee_no"),
                                        FieldDefinition.string("employeeTitle", "职员姓名").column("title")
                                )
                        )
                ),
                ModuleUiDefinition.builder("iam.user")
                        .listView(list -> list
                                .field("username")
                                .field("bound_employee", "employeeNo", field -> field.label("职员工号"))
                                .field("bound_employee", "employeeTitle", field -> field.label("职员姓名")))
                        .build(),
                List.of(new RelationProjectionJoinDefinition(
                        "bound_employee",
                        new EntityDefinition(
                                "bound_employee",
                                "iam_employee",
                                "绑定职员",
                                List.of(
                                        FieldDefinition.string("employeeNo", "职员工号").column("employee_no"),
                                        FieldDefinition.string("employeeTitle", "职员姓名").column("title")
                                )
                        ),
                        RelationProjectionCardinality.ONE_TO_ONE,
                        List.of(
                                new RelationProjectionJoinStep(
                                        "public",
                                        "iam_employee_account",
                                        "bound_employee_account",
                                        List.of(
                                                new RelationProjectionJoinCondition("main", "tenant_id",
                                                        "bound_employee_account", "tenant_id"),
                                                new RelationProjectionJoinCondition("main", "id",
                                                        "bound_employee_account", "user_id")
                                        ),
                                        List.of(new RelationProjectionJoinFilter(
                                                "bound_employee_account", "deleted", Boolean.FALSE))
                                ),
                                new RelationProjectionJoinStep(
                                        "public",
                                        "iam_employee",
                                        "bound_employee",
                                        List.of(
                                                new RelationProjectionJoinCondition("bound_employee_account", "tenant_id",
                                                        "bound_employee", "tenant_id"),
                                                new RelationProjectionJoinCondition("bound_employee_account", "employee_id",
                                                        "bound_employee", "id")
                                        ),
                                        List.of(new RelationProjectionJoinFilter(
                                                "bound_employee", "deleted", Boolean.FALSE))
                                )
                        )
                ))
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
