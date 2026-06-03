package net.ximatai.muyun.spring.iam.role;

import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.CompiledCriteria;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaSqlCompiler;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleDataScopeCriteriaServiceTest {
    private final CriteriaSqlCompiler compiler = new CriteriaSqlCompiler();

    @Test
    void shouldDenyWhenUserHasNoActionGrant() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "query")).thenReturn(List.of());
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
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "query")).thenReturn(List.of(
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
                .contains("(\"authUserId\" = :p1 OR \"authOrganizationId\" = :p2)");
        assertThat(compiled.getParams()).containsEntry("p1", "user-1").containsEntry("p2", "org-1");
    }

    @Test
    void shouldLeaveCriteriaUnrestrictedWhenAnyGrantAllowsAllData() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "query")).thenReturn(List.of(
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
    void shouldMatchCsvAssigneeAndMemberFieldsInsideServiceOnly() {
        RoleService roleService = mock(RoleService.class);
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "query")).thenReturn(List.of(
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
        when(roleService.effectiveActionGrants("user-1", "sales.contract", "query")).thenReturn(List.of(
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

    private RoleAction grant(DataScopePolicy policy) {
        RoleAction action = new RoleAction();
        action.setDataScopePolicy(policy);
        action.setEnabled(Boolean.TRUE);
        return action;
    }

    private CompiledCriteria compile(Criteria criteria) {
        return compiler.compile(criteria, field -> field, DBInfo.Type.POSTGRESQL);
    }
}
