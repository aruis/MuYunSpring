package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
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

class RelationProjectionReadServiceTest {
    @Test
    void shouldQueryRelationProjectionWithCriteriaSortFieldsAndResponseBoundary() {
        NamedParameterJdbcOperations jdbcOperations = mock(NamedParameterJdbcOperations.class);
        RelationProjectionReadService service = new RelationProjectionReadService(
                new RelationProjectionQueryExecutor(jdbcOperations),
                new RelationProjectionDatabaseTypeProvider()
        );
        StaticModuleDefinition definition = userRelationDefinition();
        RecordReadProjection projection = defaultListProjection(definition);
        Criteria criteria = Criteria.of().eq("tenantId", "tenant_a")
                .andGroup(group -> group.eq("passwordStatus", "ACTIVE"));
        when(jdbcOperations.queryForList(any(String.class), any(Map.class)))
                .thenReturn(List.of(Map.of(
                        "id", "user-1",
                        "username", "alice",
                        "employeeNo", "E001",
                        "employeeTitle", "Alice"
                )));
        when(jdbcOperations.queryForObject(any(String.class), any(Map.class), eq(Long.class)))
                .thenReturn(1L);

        PageResult<Map<String, Object>> page = service.queryList(
                definition,
                projection,
                criteria,
                PageRequest.of(1, 20),
                Sort.desc("lastLoginAt")
        ).orElseThrow();

        assertThat(page.getTotal()).isEqualTo(1);
        assertThat(page.getRecords()).singleElement()
                .satisfies(record -> assertThat(record)
                        .containsEntry("employeeNo", "E001")
                        .doesNotContainKeys("tenantId", "version", "deleted", "passwordStatus", "lastLoginAt"));
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.captor();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.captor();
        org.mockito.Mockito.verify(jdbcOperations).queryForList(sqlCaptor.capture(), paramsCaptor.capture());
        String dataSql = sqlCaptor.getValue();
        assertThat(dataSql.substring(0, dataSql.indexOf(" from (")))
                .contains("\"id\"", "\"username\"", "\"employeeNo\"", "\"employeeTitle\"")
                .doesNotContain("\"tenantId\"", "\"version\"", "\"deleted\"");
        assertThat(dataSql)
                .contains("\"main\".\"password_status\" as \"passwordStatus\"")
                .contains("\"main\".\"last_login_at\" as \"lastLoginAt\"")
                .contains("order by \"lastLoginAt\" desc");
        assertThat(paramsCaptor.getValue()).containsKeys("__limit", "__offset",
                "__join_bound_employee_bound_employee_account_0",
                "__join_bound_employee_bound_employee_0");
    }

    @Test
    void shouldRejectProjectionWithoutRelationFields() {
        RelationProjectionReadService service = new RelationProjectionReadService(
                mock(RelationProjectionQueryExecutor.class),
                new RelationProjectionDatabaseTypeProvider()
        );
        RecordReadProjection projection = new RecordReadProjection(
                "iam.user",
                "defaultList",
                List.of(ViewFieldRef.main("username")),
                List.of("id"),
                List.of()
        );

        assertThat(service.supportsListQuery(userRelationDefinition(), projection)).isFalse();
    }

    @Test
    void shouldRejectProjectionWithPostReadTransforms() {
        RelationProjectionReadService service = new RelationProjectionReadService(
                mock(RelationProjectionQueryExecutor.class),
                new RelationProjectionDatabaseTypeProvider()
        );
        RecordReadProjection projection = new RecordReadProjection(
                "iam.user",
                "defaultList",
                List.of(ViewFieldRef.main("username"), ViewFieldRef.relation("bound_employee", "employeeNo")),
                List.of("id"),
                List.of("dictionary:title")
        );

        assertThat(service.supportsListQuery(userRelationDefinition(), projection)).isFalse();
    }

    private static RecordReadProjection defaultListProjection(StaticModuleDefinition definition) {
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(definition);
        return RecordReadProjectionPlanner.defaultList(compilation.uiDescriptor(), compilation.readModel());
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
}
