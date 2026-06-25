package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.PlatformManagedMutationContext;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
import net.ximatai.muyun.spring.boot.iam.PlatformRoleActionGrantVerifier;
import net.ximatai.muyun.spring.boot.iam.PlatformSuperAdminAuthorizationInitialDataDeclarationProvider;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RoleAction;
import net.ximatai.muyun.spring.iam.role.RoleActionDao;
import net.ximatai.muyun.spring.iam.role.RoleDao;
import net.ximatai.muyun.spring.iam.role.RoleGrant;
import net.ximatai.muyun.spring.iam.role.RoleGrantDao;
import net.ximatai.muyun.spring.iam.role.RoleGrantSubjectType;
import net.ximatai.muyun.spring.iam.role.RoleKind;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantDao;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.PasswordHashingService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountDao;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import net.ximatai.muyun.spring.platform.module.ModuleKind;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleAction;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionDao;
import net.ximatai.muyun.spring.platform.module.PlatformModuleActionService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleDao;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformSuperAdminAuthorizationInitialDataDeclarationProviderTest {
    private final TenantMemoryDao tenantDao = new TenantMemoryDao();
    private final RoleMemoryDao roleDao = new RoleMemoryDao();
    private final RoleGrantMemoryDao roleGrantDao = new RoleGrantMemoryDao();
    private final RoleActionMemoryDao roleActionDao = new RoleActionMemoryDao();
    private final UserAccountMemoryDao userAccountDao = new UserAccountMemoryDao();
    private final PlatformModuleMemoryDao moduleDao = new PlatformModuleMemoryDao();
    private final PlatformModuleActionMemoryDao moduleActionDao = new PlatformModuleActionMemoryDao();
    private final TenantService tenantService = new TenantService(tenantDao);
    private final PasswordHashingService passwordHashingService = new PasswordHashingService();
    private final UserAccountService userAccountService = new UserAccountService(
            userAccountDao, tenantService, passwordHashingService);
    private final PlatformModuleService moduleService = new PlatformModuleService(moduleDao);
    private final PlatformModuleActionService moduleActionService = new PlatformModuleActionService(
            moduleActionDao, moduleService);
    private final RoleService roleService = new RoleService(
            roleDao,
            roleGrantDao,
            roleActionDao,
            tenantService,
            new PlatformRoleActionGrantVerifier(moduleService, moduleActionService),
            userAccountService,
            null,
            null,
            null
    );

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldInitializePlatformSuperAdminTenantRoleUserAndGrants() {
        registerModuleAction("iam.user", "用户", "query", "view", true);
        registerModuleAction("iam.user", "用户", "delete", "delete", true);

        initializePlatformSuperAdmin();

        Tenant tenant = tenantService.select("platform");
        assertThat(tenant).isNotNull();
        assertThat(tenant.getTitle()).isEqualTo("平台租户");
        try (TenantContext.Scope ignored = TenantContext.use("platform")) {
            Role role = roleService.select("platform.role.super_admin");
            assertThat(role).isNotNull();
            assertThat(role.getRoleKind()).isEqualTo(RoleKind.SYSTEM);
            assertThat(role.getBuiltIn()).isTrue();
            assertThat(role.getSystemManaged()).isTrue();
            UserAccount user = userAccountService.select("platform.user.super_admin");
            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("admin");
            assertThat(userAccountService.passwordMatches(user, "admin123")).isTrue();
            assertThat(roleService.userIds(role.getId())).containsExactly(user.getId());
            assertThat(roleService.hasActionPermission(user.getId(), "iam.user", "query")).isTrue();
            assertThat(roleService.hasActionPermission(user.getId(), "iam.user", "delete")).isTrue();
            RoleAction queryGrant = roleActionDao.query(Criteria.of()
                            .eq("roleId", role.getId())
                            .eq("moduleAlias", "iam.user")
                            .eq("actionCode", "view"),
                    new PageRequest(0, 1)).getFirst();
            assertThat(queryGrant.getDataScopePolicy()).isEqualTo(DataScopePolicy.ALL);
            assertThat(queryGrant.getTenantScopePolicy()).isEqualTo(TenantScopePolicy.ALL_TENANTS);
        }
    }

    @Test
    void shouldIgnoreSameAuthorizationInOtherTenantWhenInitializingPlatformTenant() {
        registerModuleAction("iam.user", "用户", "query", "view", true);
        RoleGrant otherTenantGrant = new RoleGrant();
        otherTenantGrant.setId("other-tenant-grant");
        otherTenantGrant.setTenantId("other");
        otherTenantGrant.setRoleId(RoleService.PLATFORM_SUPER_ADMIN_ROLE_ID);
        otherTenantGrant.setSubjectType(RoleGrantSubjectType.USER_ACCOUNT);
        otherTenantGrant.setSubjectId(UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);
        otherTenantGrant.setEnabled(Boolean.TRUE);
        roleGrantDao.insert(otherTenantGrant);
        RoleAction otherTenantAction = new RoleAction();
        otherTenantAction.setId("other-tenant-action");
        otherTenantAction.setTenantId("other");
        otherTenantAction.setRoleId(RoleService.PLATFORM_SUPER_ADMIN_ROLE_ID);
        otherTenantAction.setModuleAlias("iam.user");
        otherTenantAction.setActionCode("view");
        otherTenantAction.setDataScopePolicy(DataScopePolicy.ALL);
        otherTenantAction.setTenantScopePolicy(TenantScopePolicy.ALL_TENANTS);
        otherTenantAction.setEnabled(Boolean.TRUE);
        roleActionDao.insert(otherTenantAction);

        initializePlatformSuperAdmin();

        assertThat(platformRoleGrants()).hasSize(1);
        assertThat(platformRoleActions()).hasSize(1);
        assertThat(roleGrantDao.findById("other-tenant-grant").getTenantId()).isEqualTo("other");
        assertThat(roleActionDao.findById("other-tenant-action").getTenantId()).isEqualTo("other");
    }

    private void initializePlatformSuperAdmin() {
        PlatformSuperAdminAuthorizationInitialDataDeclarationProvider provider =
                new PlatformSuperAdminAuthorizationInitialDataDeclarationProvider(
                        roleService,
                        roleGrantDao,
                        roleActionDao,
                        moduleActionService
                );

        new InitialDataExecutor(
                List.<InitialDataAbility<?>>of(tenantService, roleService, userAccountService),
                List.of(provider)
        ).initializeAll();
    }

    private List<RoleGrant> platformRoleGrants() {
        return roleGrantDao.query(Criteria.of()
                        .eq("tenantId", TenantService.PLATFORM_TENANT_ID)
                        .eq("roleId", RoleService.PLATFORM_SUPER_ADMIN_ROLE_ID)
                        .eq("subjectType", RoleGrantSubjectType.USER_ACCOUNT)
                        .eq("subjectId", UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID),
                new PageRequest(0, 10));
    }

    private List<RoleAction> platformRoleActions() {
        return roleActionDao.query(Criteria.of()
                        .eq("tenantId", TenantService.PLATFORM_TENANT_ID)
                        .eq("roleId", RoleService.PLATFORM_SUPER_ADMIN_ROLE_ID)
                        .eq("moduleAlias", "iam.user")
                        .eq("actionCode", "view"),
                new PageRequest(0, 10));
    }

    private void registerModuleAction(String moduleAlias,
                                      String moduleTitle,
                                      String actionCode,
                                      String permissionActionCode,
                                      boolean dataAuth) {
        try (TenantContext.Scope ignored = TenantContext.system("register test module action")) {
            if (moduleService.select(moduleAlias) == null) {
                PlatformManagedMutationContext.runAsPlatformManaged(() -> {
                    PlatformModule module = new PlatformModule();
                    module.setAlias(moduleAlias);
                    module.setApplicationAlias(moduleAlias.substring(0, moduleAlias.indexOf('.')));
                    module.setTitle(moduleTitle);
                    module.setModuleKind(ModuleKind.STATIC);
                    module.setSystemManaged(Boolean.TRUE);
                    moduleService.insert(module);
                });
            }

            PlatformManagedMutationContext.runAsPlatformManaged(() -> {
                PlatformModuleAction action = new PlatformModuleAction();
                action.setModuleAlias(moduleAlias);
                action.setActionCode(actionCode);
                action.setPermissionActionCode(permissionActionCode);
                action.setTitle(actionCode);
                action.setActionAuth(Boolean.TRUE);
                action.setDataAuth(dataAuth);
                action.setEnabled(Boolean.TRUE);
                action.setSystemManaged(Boolean.TRUE);
                moduleActionService.insert(action);
            });
        }
    }

    private static class TenantMemoryDao extends TestMemoryDao<Tenant> implements TenantDao {
    }

    private static class RoleMemoryDao extends TestMemoryDao<Role> implements RoleDao {
    }

    private static class RoleGrantMemoryDao extends TestMemoryDao<RoleGrant> implements RoleGrantDao {
    }

    private static class RoleActionMemoryDao extends TestMemoryDao<RoleAction> implements RoleActionDao {
    }

    private static class UserAccountMemoryDao extends TestMemoryDao<UserAccount> implements UserAccountDao {
    }

    private static class PlatformModuleMemoryDao extends TestMemoryDao<PlatformModule> implements PlatformModuleDao {
    }

    private static class PlatformModuleActionMemoryDao extends TestMemoryDao<PlatformModuleAction>
            implements PlatformModuleActionDao {
    }
}
