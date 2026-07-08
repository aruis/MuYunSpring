package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StaticProjectionQueryPlannerTest {
    @Test
    void shouldPlanSqlJoinProjectionForStaticRelationFields() {
        StaticModuleDefinition definition = userDefinition();
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(definition);
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        StaticProjectionSqlPlan plan = StaticProjectionQueryPlanner.plan(definition, projection, DBInfo.Type.POSTGRESQL);

        assertThat(plan.hasRelationProjection()).isTrue();
        assertThat(plan.projectedFields()).contains(
                "id", "tenantId", "deleted", "username", "employeeNo", "employeeTitle");
        assertThat(plan.relationOutputFields()).extracting(ViewFieldRef::fieldName)
                .containsExactly("employeeNo", "employeeTitle");
        assertThat(plan.baseSql())
                .contains("from \"public\".\"iam_user\" \"main\"")
                .contains("left join \"public\".\"iam_employee_account\" \"bound_employee_account\"")
                .contains("\"main\".\"id\" = \"bound_employee_account\".\"user_id\"")
                .contains("left join \"public\".\"iam_employee\" \"bound_employee\"")
                .contains("\"bound_employee_account\".\"employee_id\" = \"bound_employee\".\"id\"")
                .contains("\"bound_employee\".\"title\" as \"employeeTitle\"");
        assertThat(plan.baseParams())
                .containsEntry("__join_bound_employee_bound_employee_account_0", Boolean.FALSE)
                .containsEntry("__join_bound_employee_bound_employee_0", Boolean.FALSE);
    }

    private StaticModuleDefinition userDefinition() {
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
                                        FieldDefinition.bool("enabled", "启用").column("enabled"),
                                        FieldDefinition.string("passwordStatus", "密码状态").column("password_status")
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
                List.of(new StaticProjectionJoinDefinition(
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
                        List.of(
                                new StaticProjectionJoinStep(
                                        "public",
                                        "iam_employee_account",
                                        "bound_employee_account",
                                        List.of(
                                                new StaticProjectionJoinCondition("main", "tenant_id",
                                                        "bound_employee_account", "tenant_id"),
                                                new StaticProjectionJoinCondition("main", "id",
                                                        "bound_employee_account", "user_id")
                                        ),
                                        List.of(new StaticProjectionJoinFilter(
                                                "bound_employee_account", "deleted", Boolean.FALSE))
                                ),
                                new StaticProjectionJoinStep(
                                        "public",
                                        "iam_employee",
                                        "bound_employee",
                                        List.of(
                                                new StaticProjectionJoinCondition("bound_employee_account", "tenant_id",
                                                        "bound_employee", "tenant_id"),
                                                new StaticProjectionJoinCondition("bound_employee_account", "employee_id",
                                                        "bound_employee", "id")
                                        ),
                                        List.of(new StaticProjectionJoinFilter(
                                                "bound_employee", "deleted", Boolean.FALSE))
                                )
                        )
                ))
        );
    }
}
