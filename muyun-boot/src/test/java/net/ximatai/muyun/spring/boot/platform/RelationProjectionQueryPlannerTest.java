package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.spring.ability.reference.ModuleReferencePath;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.user.UserAccount;
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
        assertThat(plan.queryableFields()).contains(
                "id", "tenantId", "deleted", "createdAt", "updatedAt",
                "username", "passwordStatus");
        assertThat(plan.queryableFields()).doesNotContain("employeeNo", "employeeTitle");
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
    void shouldPlanRecursiveReferencePathProjectionFromStaticReferences() {
        StaticModuleDefinition user = userReferenceDefinition();
        StaticModuleDefinition binding = employeeAccountReferenceDefinition();
        StaticModuleDefinition employee = employeeReferenceDefinition();
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(user);
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        RelationProjectionSqlPlan plan = RelationProjectionQueryPlanner.plan(
                List.of(user, binding, employee),
                user,
                projection,
                DBInfo.Type.POSTGRESQL,
                Set.of()
        );

        assertThat(plan.hasRelationProjection()).isTrue();
        assertThat(plan.queryableFields()).contains("id", "username", "employeeNo");
        assertThat(plan.queryableFields()).doesNotContain("employeeTitle");
        assertThat(plan.sortableFields()).contains("id", "username", "employeeNo", "employeeTitle");
        assertThat(plan.responseFields()).containsExactlyInAnyOrder("id", "username", "employeeNo", "employeeTitle");
        assertThat(plan.baseSql())
                .contains("left join \"public\".\"iam_employee_account\" \"user_id\"")
                .contains("\"main\".\"id\" = \"user_id\".\"user_id\"")
                .contains("left join \"public\".\"iam_employee\" \"user_id_employee_id\"")
                .contains("\"user_id\".\"employee_id\" = \"user_id_employee_id\".\"id\"")
                .contains("\"user_id_employee_id\".\"employee_no\" as \"employeeNo\"")
                .contains("\"user_id_employee_id\".\"title\" as \"employeeTitle\"");
        assertThat(plan.baseParams())
                .containsEntry("__join_user_id_deleted", Boolean.FALSE)
                .containsEntry("__join_user_id_employee_id_deleted", Boolean.FALSE);
    }

    @Test
    void shouldPlanDirectReferenceProjectionFromStaticReferences() {
        StaticModuleDefinition employee = employeeWithOrganizationProjectionDefinition();
        StaticModuleDefinition organization = organizationReferenceDefinition();
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(employee);
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        RelationProjectionSqlPlan plan = RelationProjectionQueryPlanner.plan(
                List.of(employee, organization),
                employee,
                projection,
                DBInfo.Type.POSTGRESQL,
                Set.of()
        );

        assertThat(plan.hasRelationProjection()).isTrue();
        assertThat(plan.queryableFields()).doesNotContain("organizationTitle");
        assertThat(plan.sortableFields()).contains("organizationTitle");
        assertThat(plan.responseFields()).containsExactlyInAnyOrder(
                "id", "employeeNo", "organizationTitle", "title");
        assertThat(plan.baseSql())
                .contains("left join \"public\".\"iam_organization\" \"organization\"")
                .contains("\"main\".\"organization_id\" = \"organization\".\"id\"")
                .contains("\"organization\".\"title\" as \"organizationTitle\"");
        assertThat(plan.baseParams())
                .containsEntry("__join_organization_deleted", Boolean.FALSE);
    }

    @Test
    void shouldRejectReadProjectionOutputConflictWithMainField() {
        assertThatThrownBy(() -> userReferenceDefinitionWithOutput("employee_account.employee.title", "username"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("read projection output field conflicts with main field: iam.user.username");
    }

    @Test
    void shouldRejectDuplicateReadProjectionOutputField() {
        assertThatThrownBy(this::duplicateReadProjectionOutputDefinition)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate static module read projection output field: iam.employee.organizationTitle");
    }

    @Test
    void shouldRejectDuplicateReferenceCode() {
        assertThatThrownBy(this::duplicateReferenceCodeDefinition)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate static module reference code: iam.employee.organization");
    }

    @Test
    void shouldRejectReadProjectionPathCannotResolve() {
        StaticModuleDefinition user = userReferenceDefinitionWithOutput("missing.employee.title", "employeeTitle");
        StaticModuleDefinition binding = employeeAccountReferenceDefinition();
        StaticModuleDefinition employee = employeeReferenceDefinition();
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(user);
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        assertThatThrownBy(() -> RelationProjectionQueryPlanner.plan(
                List.of(user, binding, employee),
                user,
                projection,
                DBInfo.Type.POSTGRESQL,
                Set.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projection reference path is not declared: "
                        + "iam.user.employeeTitle.missing.employee");
    }

    @Test
    void shouldRejectMissingReferenceTargetField() {
        StaticModuleDefinition employee = employeeWithReadProjectionDefinition("organization.missingTitle",
                "organizationTitle");
        StaticModuleDefinition organization = organizationReferenceDefinition();
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(employee);
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        assertThatThrownBy(() -> RelationProjectionQueryPlanner.plan(
                List.of(employee, organization),
                employee,
                projection,
                DBInfo.Type.POSTGRESQL,
                Set.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projection reference field is not declared: organization.missingTitle");
    }

    @Test
    void shouldRejectUnsafeInverseReferencePathProjection() {
        StaticModuleDefinition user = userReferenceDefinitionWithOutput(
                ModuleReferencePath.inverse(EmployeeAccount::getUserId)
                        .then(EmployeeAccount::getEmployeeId)
                        .select(Employee::getTitle),
                "employeeTitle");
        StaticModuleDefinition binding = employeeAccountReferenceDefinition();
        StaticModuleDefinition employee = employeeReferenceDefinition();
        ModuleUiCompilationResult compilation = ModuleUiDescriptorCompiler.compileModule(user);
        RecordReadProjection projection = RecordReadProjectionPlanner.defaultList(
                compilation.uiDescriptor(),
                compilation.readModel()
        );

        assertThatThrownBy(() -> RelationProjectionQueryPlanner.plan(
                List.of(user, binding, employee),
                user,
                projection,
                DBInfo.Type.POSTGRESQL,
                Set.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("projection reference path cardinality is not safe for page join")
                .hasMessageContaining("ONE_TO_MANY");
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

    private StaticModuleDefinition userReferenceDefinition() {
        return userReferenceDefinitionWithOutput(
                ModuleReferencePath.inverseOne(EmployeeAccount::getUserId)
                        .then(EmployeeAccount::getEmployeeId)
                        .select(Employee::getTitle),
                "employeeTitle");
    }

    private StaticModuleDefinition userReferenceDefinitionWithOutput(String readProjectionPath, String outputField) {
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
                List.of(new EntityDefinition(
                        "user",
                        "iam_user",
                        "User",
                        List.of(FieldDefinition.string("username", "账号").column("username"))
                )),
                ModuleUiDefinition.builder("iam.user")
                        .listView(list -> list
                                .field("username")
                                .field("employeeNo")
                                .field(outputField))
                        .build(),
                List.of(),
                List.of(
                        new StaticModuleReadProjectionDefinition(
                                null,
                                ModuleReferencePath.inverseOne(EmployeeAccount::getUserId)
                                        .then(EmployeeAccount::getEmployeeId)
                                        .select(Employee::getEmployeeNo),
                                "employeeNo",
                                net.ximatai.muyun.spring.ability.reference.ModuleReadProjection.ProjectionType.FIELD,
                                true,
                                true
                        ),
                        new StaticModuleReadProjectionDefinition(
                                readProjectionPath,
                                outputField
                        )
                ),
                UserAccount.class,
                List.of()
        );
    }

    private StaticModuleDefinition userReferenceDefinitionWithOutput(ModuleReferencePath readProjectionPath,
                                                                     String outputField) {
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
                List.of(new EntityDefinition(
                        "user",
                        "iam_user",
                        "User",
                        List.of(FieldDefinition.string("username", "账号").column("username"))
                )),
                ModuleUiDefinition.builder("iam.user")
                        .listView(list -> list
                                .field("username")
                                .field("employeeNo")
                                .field(outputField))
                        .build(),
                List.of(),
                List.of(
                        new StaticModuleReadProjectionDefinition(
                                null,
                                ModuleReferencePath.inverseOne(EmployeeAccount::getUserId)
                                        .then(EmployeeAccount::getEmployeeId)
                                        .select(Employee::getEmployeeNo),
                                "employeeNo",
                                net.ximatai.muyun.spring.ability.reference.ModuleReadProjection.ProjectionType.FIELD,
                                true,
                                true
                        ),
                        new StaticModuleReadProjectionDefinition(
                                readProjectionPath,
                                outputField
                        )
                ),
                UserAccount.class,
                List.of()
        );
    }

    private StaticModuleDefinition employeeAccountReferenceDefinition() {
        return new StaticModuleDefinition(
                "iam",
                "iam.employee_account",
                "职员账号绑定",
                null,
                ModuleEntryType.MODULE,
                null,
                null,
                Set.of(EntityCapability.CRUD),
                List.of(),
                List.of(new EntityDefinition(
                        "employee_account",
                        "iam_employee_account",
                        "Employee Account",
                        List.of(
                                FieldDefinition.string("employeeId", "职员").column("employee_id"),
                                FieldDefinition.string("userId", "用户").column("user_id")
                        )
                )),
                null,
                List.of(
                        new StaticModuleReferenceDefinition("employee", "employeeId", "iam.employee", "id"),
                        new StaticModuleReferenceDefinition("user", "userId", "iam.user", "id")
                ),
                List.of(),
                EmployeeAccount.class,
                List.of()
        );
    }

    private StaticModuleDefinition employeeReferenceDefinition() {
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
                                FieldDefinition.string("employeeNo", "职员编号").column("employee_no"),
                                FieldDefinition.string("title", "职员姓名").column("title")
                        )
                )),
                null,
                List.of(),
                List.of(),
                Employee.class,
                List.of()
        );
    }

    private StaticModuleDefinition employeeWithOrganizationProjectionDefinition() {
        return employeeWithReadProjectionDefinition("organization.title", "organizationTitle");
    }

    private StaticModuleDefinition employeeWithReadProjectionDefinition(String readProjectionPath, String outputField) {
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
                                FieldDefinition.string("organizationId", "所属机构").column("organization_id"),
                                FieldDefinition.string("employeeNo", "职员编号").column("employee_no"),
                                FieldDefinition.string("title", "职员姓名").column("title")
                        )
                )),
                ModuleUiDefinition.builder("iam.employee")
                        .listView(list -> list
                                .field("employeeNo")
                                .field(outputField)
                                .field("title"))
                        .build(),
                List.of(new StaticModuleReferenceDefinition("organization", "organizationId", "iam.organization", "id")),
                List.of(new StaticModuleReadProjectionDefinition(readProjectionPath, outputField)),
                Employee.class,
                List.of()
        );
    }

    private StaticModuleDefinition organizationReferenceDefinition() {
        return new StaticModuleDefinition(
                "iam",
                "iam.organization",
                "机构管理",
                null,
                ModuleEntryType.ROUTE,
                "/iam/organizations",
                null,
                Set.of(EntityCapability.CRUD),
                List.of(),
                List.of(new EntityDefinition(
                        "organization",
                        "iam_organization",
                        "Organization",
                        List.of(FieldDefinition.string("title", "机构名称").column("title"))
                )),
                null,
                List.of(),
                List.of(),
                Organization.class,
                List.of()
        );
    }

    private StaticModuleDefinition duplicateReadProjectionOutputDefinition() {
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
                        List.of(FieldDefinition.string("employeeNo", "职员编号").column("employee_no"))
                )),
                null,
                List.of(),
                List.of(
                        new StaticModuleReadProjectionDefinition("organization.title", "organizationTitle"),
                        new StaticModuleReadProjectionDefinition("department.title", "organizationTitle")
                )
        );
    }

    private StaticModuleDefinition duplicateReferenceCodeDefinition() {
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
                                FieldDefinition.string("organizationId", "所属机构").column("organization_id"),
                                FieldDefinition.string("departmentId", "所属部门").column("department_id")
                        )
                )),
                null,
                List.of(
                        new StaticModuleReferenceDefinition("organization", "organizationId", "iam.organization", "id"),
                        new StaticModuleReferenceDefinition("organization", "departmentId", "iam.department", "id")
                ),
                List.of()
        );
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
