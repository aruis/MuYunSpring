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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelationProjectionQueryPlannerTest {
    @Test
    void shouldPlanSqlJoinProjectionForStaticRelationFields() {
        StaticModuleDefinition definition = userDefinition();
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(definition);
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        RelationProjectionSqlPlan plan = RelationProjectionQueryPlanner.plan(
                definition,
                projection,
                DBInfo.Type.POSTGRESQL,
                Set.of("passwordStatus", "createdAt")
        );

        assertThat(plan.hasRelationProjection()).isTrue();
        assertThat(plan.projectedFields()).contains(
                "id", "tenantId", "deleted", "createdAt", "updatedAt",
                "username", "passwordStatus");
        assertThat(plan.projectedFields()).doesNotContain("employeeNo", "employeeTitle");
        assertThat(plan.responseFields()).containsExactlyInAnyOrder(
                "id", "username", "employeeNo", "employeeTitle");
        assertThat(plan.responseFields()).doesNotContain("tenantId", "version", "deleted");
        assertThat(plan.relationOutputFields()).extracting(ViewFieldRef::fieldName)
                .containsExactly("employeeNo", "employeeTitle");
        assertThat(plan.baseSql())
                .contains("from \"public\".\"iam_user\" \"main\"")
                .contains("\"main\".\"password_status\" as \"passwordStatus\"")
                .contains("left join \"public\".\"iam_employee_account\" \"bound_employee_account\"")
                .contains("\"main\".\"id\" = \"bound_employee_account\".\"user_id\"")
                .contains("left join \"public\".\"iam_employee\" \"bound_employee\"")
                .contains("\"bound_employee_account\".\"employee_id\" = \"bound_employee\".\"id\"")
                .contains("\"bound_employee\".\"title\" as \"employeeTitle\"");
        assertThat(plan.baseSql()).doesNotContain("\"main\".\"enabled\" as \"enabled\"");
        assertThat(plan.baseParams())
                .containsEntry("__join_bound_employee_bound_employee_account_0", Boolean.FALSE)
                .containsEntry("__join_bound_employee_bound_employee_0", Boolean.FALSE);
    }

    @Test
    void shouldRejectUnsafePageJoinCardinality() {
        StaticModuleDefinition definition = userDefinition(RelationProjectionCardinality.ONE_TO_MANY);
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(definition);
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        assertThatThrownBy(() -> RelationProjectionQueryPlanner.plan(definition, projection, DBInfo.Type.POSTGRESQL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cardinality is not safe for page join");
    }

    @Test
    void shouldRequireExplicitJoinCardinality() {
        assertThatThrownBy(() -> userDefinition(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projection join cardinality must not be null");
    }

    private StaticModuleDefinition userDefinition() {
        return userDefinition(RelationProjectionCardinality.ONE_TO_ONE);
    }

    private StaticModuleDefinition userDefinition(RelationProjectionCardinality cardinality) {
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
                        cardinality,
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
