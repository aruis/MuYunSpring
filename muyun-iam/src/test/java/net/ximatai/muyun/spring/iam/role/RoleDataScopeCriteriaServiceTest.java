package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.CompiledCriteria;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaSqlCompiler;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopePlan;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopeRequest;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopeResolver;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleDataScopeCriteriaServiceTest {
    private final CriteriaSqlCompiler compiler = new CriteriaSqlCompiler();

    @Test
    void shouldDenyWhenUserHasNoActionGrant() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of());
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql()).contains("\"status\" = :p0").contains("1 = 0");
    }

    @Test
    void shouldUnionOwnerAndOrganizationScopes() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.OWNER),
                grant(DataScopePolicy.ORGANIZATION)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a", "org-1"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"authUserId\" = :p1")
                .contains("\"authOrganizationId\" = :p2");
        assertThat(compiled.getParams()).containsEntry("p1", "user-1").containsEntry("p2", "org-1");
    }

    @Test
    void shouldLeaveCriteriaUnrestrictedWhenAnyGrantAllowsAllData() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.OWNER),
                grant(DataScopePolicy.ALL)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a", "org-1"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql()).isEqualTo("\"status\" = :p0");
    }

    @Test
    void shouldMarkAllDataScopeAsCrossTenantWhenRoleAllowsAllTenants() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.ALL, "role-cross", TenantScopePolicy.ALL_TENANTS)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        DataScopeCriteriaResult result = service.resolveReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        assertThat(result.restricted()).isFalse();
        assertThat(result.crossTenant()).isTrue();
        assertThat(compile(result.criteria()).getSql()).isEqualTo("\"status\" = :p0");
    }

    @Test
    void shouldNotMarkCrossTenantWhenAllTenantRoleDoesNotContributeActionGrant() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.ALL, "role-current")
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        DataScopeCriteriaResult result = service.resolveReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        assertThat(result.restricted()).isFalse();
        assertThat(result.crossTenant()).isFalse();
        assertThat(compile(result.criteria()).getSql()).isEqualTo("\"status\" = :p0");
    }

    @Test
    void shouldKeepCurrentTenantAllScopedWhenMixedWithCrossTenantRestrictedGrant() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.ALL, "role-current"),
                grant(DataScopePolicy.OWNER, "role-cross", TenantScopePolicy.ALL_TENANTS)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        DataScopeCriteriaResult result = service.resolveReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(result.criteria());
        assertThat(result.restricted()).isTrue();
        assertThat(result.crossTenant()).isTrue();
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"tenantId\" = :p1")
                .contains("\"authUserId\" = :p2");
        assertThat(compiled.getParams()).containsEntry("p1", "tenant-a").containsEntry("p2", "user-1");
    }

    @Test
    void shouldNotMarkCrossTenantWhenAllTenantGrantContributesNoScope() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.ALL, "role-current"),
                grant(DataScopePolicy.NONE, "role-cross", TenantScopePolicy.ALL_TENANTS)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        DataScopeCriteriaResult result = service.resolveReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(result.criteria());
        assertThat(result.restricted()).isTrue();
        assertThat(result.crossTenant()).isFalse();
        assertThat(compiled.getSql()).contains("\"tenantId\" = :p1");
        assertThat(compiled.getParams()).containsEntry("p1", "tenant-a");
    }

    @Test
    void shouldKeepBusinessScopeWhenRoleAllowsAllTenants() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.OWNER, "role-cross", TenantScopePolicy.ALL_TENANTS)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        DataScopeCriteriaResult result = service.resolveReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(result.criteria());
        assertThat(result.restricted()).isTrue();
        assertThat(result.crossTenant()).isTrue();
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"authUserId\" = :p1");
    }

    @Test
    void shouldDenyWhenOnlyGrantHasNoDataScope() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.NONE)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql()).contains("\"status\" = :p0").contains("1 = 0");
    }

    @Test
    void shouldApplyOrganizationAndChildrenScope() {
        RoleService roleService = mock(RoleService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.ORGANIZATION_AND_CHILDREN)
        ));
        when(organizationService.selfAndDescendantIds("org-1")).thenReturn(List.of("org-1", "org-1-1"));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService, Optional.of(organizationService));

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a", "org-1"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"authOrganizationId\" IN (:p1_0, :p1_1)");
        assertThat(compiled.getParams()).containsEntry("p1_0", "org-1").containsEntry("p1_1", "org-1-1");
    }

    @Test
    void shouldResolveOrganizationScopeFromEffectiveRoleGrantContext() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrantsWithContext("user-1", "sales.contract", "view")).thenReturn(List.of(
                effectiveActionGrant(grant(DataScopePolicy.ORGANIZATION, "employee-role"),
                        effectiveRoleGrant("employee-role", RoleGrantSubjectType.EMPLOYEE,
                                "employee-1", "org-main", "dept-main", null)),
                effectiveActionGrant(grant(DataScopePolicy.ORGANIZATION, "position-role"),
                        effectiveRoleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION,
                                "position-1", "org-branch", "dept-branch", "position-1"))
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a", "org-account"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"authOrganizationId\" = :p1")
                .contains("\"authOrganizationId\" = :p2");
        assertThat(compiled.getParams())
                .containsEntry("p1", "org-main")
                .containsEntry("p2", "org-branch");
    }

    @Test
    void shouldResolveAccountRoleOrganizationScopeFromCurrentUserWhenContextHasNoOrganization() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrantsWithContext("user-1", "sales.contract", "view")).thenReturn(List.of(
                effectiveActionGrant(grant(DataScopePolicy.ORGANIZATION, "account-role"),
                        effectiveRoleGrant("account-role", RoleGrantSubjectType.USER_ACCOUNT,
                                "user-1", null, null, null))
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a", "org-account"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"authOrganizationId\" = :p1");
        assertThat(compiled.getParams()).containsEntry("p1", "org-account");
    }

    @Test
    void shouldApplyOrganizationChildrenScopePerEffectiveRoleGrantContext() {
        RoleService roleService = mock(RoleService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        when(roleService.effectiveActionGrantsWithContext("user-1", "sales.contract", "view")).thenReturn(List.of(
                effectiveActionGrant(grant(DataScopePolicy.ORGANIZATION_AND_CHILDREN, "employee-role"),
                        effectiveRoleGrant("employee-role", RoleGrantSubjectType.EMPLOYEE,
                                "employee-1", "org-main", "dept-main", null)),
                effectiveActionGrant(grant(DataScopePolicy.ORGANIZATION_AND_CHILDREN, "position-role"),
                        effectiveRoleGrant("position-role", RoleGrantSubjectType.EMPLOYEE_POSITION,
                                "position-1", "org-branch", "dept-branch", "position-1"))
        ));
        when(organizationService.selfAndDescendantIds("org-main")).thenReturn(List.of("org-main", "org-main-child"));
        when(organizationService.selfAndDescendantIds("org-branch")).thenReturn(List.of("org-branch"));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService, Optional.of(organizationService));

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a", "org-account"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"authOrganizationId\" IN (:p1_0, :p1_1)")
                .contains("\"authOrganizationId\" IN (:p2_0)");
        assertThat(compiled.getParams().values())
                .contains("org-main", "org-main-child", "org-branch")
                .doesNotContain("org-account");
    }

    @Test
    void shouldFailFastWhenOrganizationAndChildrenScopeHasNoOrganizationService() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.ORGANIZATION_AND_CHILDREN)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService, Optional.empty());

        assertThatThrownBy(() -> service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of(),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a", "org-1"))
        )).isInstanceOf(PlatformException.class)
                .hasMessageContaining("organization hierarchy support");
    }

    @Test
    void shouldMatchCsvAssigneeAndMemberFieldsInsideServiceOnly() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.ASSIGNEE),
                grant(DataScopePolicy.MEMBER)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of(),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("CONCAT(',', auth_assignee_ids, ',') LIKE")
                .contains("CONCAT(',', auth_member_ids, ',') LIKE");
        assertThat(compiled.getParams().values()).containsOnly("%,user-1,%");
    }

    @Test
    void shouldCompileCsvScopeForMysqlAsCurrentSupportedRawShape() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(
                grant(DataScopePolicy.ASSIGNEE)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of(),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compiler.compile(scoped, field -> field, DBInfo.Type.MYSQL);
        assertThat(compiled.getSql()).contains("CONCAT(',', auth_assignee_ids, ',') LIKE");
        assertThat(compiled.getParams().values()).containsOnly("%,user-1,%");
    }

    @Test
    void shouldApplyDefaultOwnerScopeWithoutRoleGrant() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "follow")).thenReturn(List.of());
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        DataScopeCriteriaResult result = service.resolveReadScope(
                "sales.contract",
                policy("follow", ActionDefaultGrantPolicy.OWNER),
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(result.criteria());
        assertThat(result.restricted()).isTrue();
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"authUserId\" = :p1");
        assertThat(compiled.getParams()).containsEntry("p1", "user-1");
    }

    @Test
    void shouldExpandDefaultMemberScopeToOwnerAssigneeAndMember() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "follow")).thenReturn(List.of());
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.resolveReadScope(
                "sales.contract",
                policy("follow", ActionDefaultGrantPolicy.MEMBER),
                Criteria.of(),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        ).criteria();

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("\"authUserId\" = :p0")
                .contains("CONCAT(',', auth_assignee_ids, ',') LIKE")
                .contains("CONCAT(',', auth_member_ids, ',') LIKE");
    }

    @Test
    void shouldNotTreatAnyLoginUserDefaultGrantAsDataScope() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "query")).thenReturn(List.of());
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.resolveReadScope(
                "sales.contract",
                policy("query", ActionDefaultGrantPolicy.ANY_LOGIN_USER),
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        ).criteria();

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql()).contains("\"status\" = :p0").contains("1 = 0");
    }

    @Test
    void shouldResolveWildcardDataScopeThroughBoundWildcardRole() {
        RoleService roleService = mock(RoleService.class);
        RoleAction wildcard = grant(DataScopePolicy.WILDCARD);
        wildcard.setActionCode("view");
        RoleAction actual = grant(DataScopePolicy.OWNER);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(wildcard));
        when(roleService.effectiveWildcardDataScopeGrant("user-1", "view")).thenReturn(actual);
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"authUserId\" = :p1");
        assertThat(compiled.getParams()).containsEntry("p1", "user-1");
    }

    @Test
    void shouldNotLetWildcardGrantWidenTenantScopeBeyondOriginalGrant() {
        RoleService roleService = mock(RoleService.class);
        RoleAction wildcard = grant(DataScopePolicy.WILDCARD);
        wildcard.setActionCode("view");
        RoleAction actual = grant(DataScopePolicy.ALL, "scope-role", TenantScopePolicy.ALL_TENANTS);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(wildcard));
        when(roleService.effectiveWildcardDataScopeGrant("user-1", "view")).thenReturn(actual);
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        DataScopeCriteriaResult result = service.resolveReadScope(
                "sales.contract",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(result.criteria());
        assertThat(result.crossTenant()).isFalse();
        assertThat(compiled.getSql()).isEqualTo("\"status\" = :p0");
    }

    @Test
    void shouldDenyWildcardDataScopeWhenNoWildcardRoleGrantExists() {
        RoleService roleService = mock(RoleService.class);
        RoleAction wildcard = grant(DataScopePolicy.WILDCARD);
        wildcard.setActionCode("view");
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "view")).thenReturn(List.of(wildcard));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.contract",
                "query",
                Criteria.of(),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        assertThat(compile(scoped).getSql()).contains("1 = 0");
    }

    @Test
    void shouldApplyReferenceDependencyScopeAsTargetPermissionSubquery() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.score", "view")).thenReturn(List.of(
                referenceGrant("studentId", "view")
        ));
        when(roleService.effectiveActionGrants("user-1", "school.student", "view")).thenReturn(List.of(
                grant(DataScopePolicy.OWNER)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(
                roleService,
                Optional.empty(),
                Optional.of(referenceResolver("studentId", "school.student", "student"))
        );

        Criteria scoped = service.applyReadScope(
                "sales.score",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(scoped);
        assertThat(compiled.getSql())
                .contains("\"status\" = :p0")
                .contains("\"studentId\" IN (SELECT \"id\" FROM \"public\".\"school_student\"")
                .contains("\"auth_user_id\" =")
                .contains("\"tenant_id\" =")
                .contains("\"deleted\" =");
        assertThat(compiled.getParams().values()).contains("OPEN", "user-1", "tenant-a", Boolean.FALSE);
    }

    @Test
    void shouldDefaultReferenceDependencyActionToReference() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.score", "view")).thenReturn(List.of(
                referenceGrant("studentId", null)
        ));
        when(roleService.effectiveActionGrants("user-1", "school.student", "view")).thenReturn(List.of(
                grant(DataScopePolicy.OWNER)
        ));
        AtomicReference<String> requestedAction = new AtomicReference<>();
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(
                roleService,
                Optional.empty(),
                Optional.of(referenceResolver("studentId", "school.student", "student", requestedAction))
        );

        Criteria scoped = service.applyReadScope(
                "sales.score",
                "query",
                Criteria.of(),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        assertThat(compile(scoped).getSql()).contains("\"studentId\" IN (SELECT \"id\"");
        assertThat(requestedAction).hasValue(PlatformAction.REFERENCE.code());
    }

    @Test
    void shouldKeepReferenceDependencyCurrentTenantWhenTargetScopeIsNotCrossTenant() {
        RoleService roleService = mock(RoleService.class);
        RoleAction sourceGrant = referenceGrant("studentId", "view");
        sourceGrant.setTenantScopePolicy(TenantScopePolicy.ALL_TENANTS);
        when(roleService.effectiveActionGrants("user-1", "sales.score", "view")).thenReturn(List.of(sourceGrant));
        when(roleService.effectiveActionGrants("user-1", "school.student", "view")).thenReturn(List.of(
                grant(DataScopePolicy.OWNER)
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(
                roleService,
                Optional.empty(),
                Optional.of(referenceResolver("studentId", "school.student", "student"))
        );

        DataScopeCriteriaResult result = service.resolveReadScope(
                "sales.score",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        CompiledCriteria compiled = compile(result.criteria());
        assertThat(result.crossTenant()).isFalse();
        assertThat(compiled.getSql())
                .contains("\"tenantId\" =")
                .contains("\"studentId\" IN (SELECT \"id\"");
        assertThat(compiled.getParams().values()).contains("tenant-a");
    }

    @Test
    void shouldDenyReferenceDependencyWhenResolverCannotResolve() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.score", "view")).thenReturn(List.of(
                referenceGrant("missingField", "view")
        ));
        RoleDataScopeCriteriaService service = new RoleDataScopeCriteriaService(roleService);

        Criteria scoped = service.applyReadScope(
                "sales.score",
                "query",
                Criteria.of().eq("status", "OPEN"),
                Optional.of(CurrentUser.tenantUser("user-1", "User", "tenant-a"))
        );

        assertThat(compile(scoped).getSql()).contains("1 = 0");
    }

    private RoleAction grant(DataScopePolicy policy) {
        return grant(policy, "role-1");
    }

    private RoleAction grant(DataScopePolicy policy, String roleId) {
        return grant(policy, roleId, TenantScopePolicy.CURRENT_TENANT);
    }

    private RoleAction grant(DataScopePolicy policy, String roleId, TenantScopePolicy tenantScopePolicy) {
        RoleAction action = new RoleAction();
        action.setRoleId(roleId);
        action.setDataScopePolicy(policy);
        action.setTenantScopePolicy(tenantScopePolicy);
        action.setEnabled(Boolean.TRUE);
        return action;
    }

    private RoleAction referenceGrant(String referenceFieldId, String referenceActionCode) {
        RoleAction action = grant(DataScopePolicy.REFERENCE_DEPENDENCY);
        action.setReferenceFieldId(referenceFieldId);
        action.setReferenceActionCode(referenceActionCode);
        return action;
    }

    private EffectiveRoleGrant effectiveRoleGrant(String roleId,
                                                  RoleGrantSubjectType sourceType,
                                                  String sourceId,
                                                  String organizationId,
                                                  String departmentId,
                                                  String employeePositionId) {
        return new EffectiveRoleGrant(roleId, sourceType, sourceId, organizationId, departmentId, employeePositionId);
    }

    private EffectiveRoleActionGrant effectiveActionGrant(RoleAction actionGrant, EffectiveRoleGrant roleGrant) {
        return new EffectiveRoleActionGrant(actionGrant, roleGrant);
    }

    private ReferenceDependencyScopeResolver referenceResolver(String sourceField,
                                                              String targetModuleAlias,
                                                              String targetEntityAlias) {
        return referenceResolver(sourceField, targetModuleAlias, targetEntityAlias, new AtomicReference<>());
    }

    private ReferenceDependencyScopeResolver referenceResolver(String sourceField,
                                                              String targetModuleAlias,
                                                              String targetEntityAlias,
                                                              AtomicReference<String> requestedAction) {
        return request -> {
            requestedAction.set(request.referenceActionCode());
            if (!sourceField.equals(request.referenceFieldId())) {
                return Optional.empty();
            }
            return Optional.of(new ReferenceDependencyScopePlan(
                    sourceField,
                    targetModuleAlias,
                    targetEntityAlias,
                    "public",
                    "school_student",
                    Map.of(
                            "id", "id",
                            "tenantId", "tenant_id",
                            "deleted", "deleted",
                            "authUserId", "auth_user_id"
                    ),
                    DBInfo.Type.POSTGRESQL
            ));
        };
    }

    private CompiledCriteria compile(Criteria criteria) {
        return compiler.compile(criteria, field -> field, DBInfo.Type.POSTGRESQL);
    }

    private ActionExecutionPolicy policy(String actionCode, ActionDefaultGrantPolicy defaultGrantPolicy) {
        return new ActionExecutionPolicy(
                actionCode,
                PlatformActionLevel.RECORD,
                ActionAccessMode.AUTH_REQUIRED,
                true,
                true,
                defaultGrantPolicy,
                null
        );
    }
}
