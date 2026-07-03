package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.MuYunSpringDemoBootstrapProperties;
import net.ximatai.muyun.spring.boot.iam.BuiltInRolePermissionTemplateService;
import net.ximatai.muyun.spring.boot.iam.RoleGrantableActionResolver;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.OrganizationCreationProvisioner;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentDao;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountDao;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeDao;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationDao;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrant;
import net.ximatai.muyun.spring.iam.role.AccountRoleGrantDao;
import net.ximatai.muyun.spring.iam.role.EmploymentRoleGrant;
import net.ximatai.muyun.spring.iam.role.EmploymentRoleGrantDao;
import net.ximatai.muyun.spring.iam.role.ManagementScopeType;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RoleAction;
import net.ximatai.muyun.spring.iam.role.RoleActionDao;
import net.ximatai.muyun.spring.iam.role.RoleAssignmentType;
import net.ximatai.muyun.spring.iam.role.RoleDao;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.RoleSharePolicy;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.role.RoleOwnerScopeType;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantDao;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.PasswordHashingService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountDao;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoBootstrapTaskTest {
    private final TenantMemoryDao tenantDao = new TenantMemoryDao();
    private final OrganizationMemoryDao organizationDao = new OrganizationMemoryDao();
    private final DepartmentMemoryDao departmentDao = new DepartmentMemoryDao();
    private final EmployeeMemoryDao employeeDao = new EmployeeMemoryDao();
    private final UserAccountMemoryDao userAccountDao = new UserAccountMemoryDao();
    private final EmployeeAccountMemoryDao employeeAccountDao = new EmployeeAccountMemoryDao();
    private final RoleMemoryDao roleDao = new RoleMemoryDao();
    private final AccountRoleGrantMemoryDao accountRoleGrantDao = new AccountRoleGrantMemoryDao();
    private final EmploymentRoleGrantMemoryDao employmentRoleGrantDao = new EmploymentRoleGrantMemoryDao();
    private final RoleActionMemoryDao roleActionDao = new RoleActionMemoryDao();

    private final TenantService tenantService = new TenantService(tenantDao);
    private final OrganizationService organizationService = new OrganizationService(organizationDao, tenantService);
    private final DepartmentService departmentService = new DepartmentService(departmentDao, tenantService,
            organizationService);
    private final EmployeeService employeeService = new EmployeeService(employeeDao, tenantService, organizationService,
            departmentService);
    private final UserAccountService userAccountService = new UserAccountService(userAccountDao, tenantService,
            new PasswordHashingService());
    private final EmployeeAccountService employeeAccountService = new EmployeeAccountService(employeeAccountDao,
            tenantService, employeeService, userAccountService);
    private final RoleService roleService = new RoleService(roleDao, accountRoleGrantDao, employmentRoleGrantDao,
            roleActionDao, tenantService, net.ximatai.muyun.spring.iam.role.RoleActionGrantVerifier.platformActionsOnly(),
            userAccountService, employeeService, null, employeeAccountService);
    private final RoleGrantableActionResolver grantableActionResolver = mock(RoleGrantableActionResolver.class);
    private final BuiltInRolePermissionTemplateService rolePermissionTemplateService =
            new BuiltInRolePermissionTemplateService(roleService, grantableActionResolver);
    private final DefaultTenantRoleProvisioner tenantRoleProvisioner =
            new DefaultTenantRoleProvisioner(roleService, rolePermissionTemplateService);
    private final DefaultOrganizationRoleProvisioner organizationRoleProvisioner =
            new DefaultOrganizationRoleProvisioner(roleService, rolePermissionTemplateService);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDoNothingWhenDemoBootstrapIsDisabled() {
        DemoBootstrapTask task = new DemoBootstrapTask(new MuYunSpringDemoBootstrapProperties(),
                tenantService, organizationService, departmentService, employeeService, userAccountService,
                employeeAccountService, tenantRoleProvisioner);

        task.run();

        assertThat(tenantDao.list(Criteria.of())).isEmpty();
        assertThat(organizationDao.list(Criteria.of())).isEmpty();
        assertThat(departmentDao.list(Criteria.of())).isEmpty();
        assertThat(employeeDao.list(Criteria.of())).isEmpty();
        assertThat(userAccountDao.list(Criteria.of())).isEmpty();
        assertThat(roleDao.list(Criteria.of())).isEmpty();
    }

    @Test
    void shouldProvisionTenantAdminRoleWhenTenantIsCreated() {
        when(grantableActionResolver.resolve(any())).thenReturn(List.of(
                GrantableAction.ofPlatformDefaults("iam.user", PlatformAction.QUERY)
        ));
        @SuppressWarnings("unchecked")
        ObjectProvider<net.ximatai.muyun.spring.common.tenant.TenantCreationProvisioner> provisioners =
                mock(ObjectProvider.class);
        when(provisioners.orderedStream()).thenAnswer(invocation -> Stream.of(tenantRoleProvisioner));
        TenantService provisioningTenantService = new TenantService(tenantDao, provisioners);
        Tenant tenant = new Tenant();
        tenant.setAlias("acme");
        tenant.setTitle("Acme");
        tenant.setEnabled(Boolean.TRUE);

        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            provisioningTenantService.insert(tenant);
        }

        try (TenantContext.Scope ignored = TenantContext.use("acme")) {
            Role role = roleService.select(DefaultTenantRoleProvisioner.tenantAdminRoleId("acme"));
            assertThat(role).isNotNull();
            assertThat(role.getTitle()).isEqualTo(RoleService.TENANT_ADMIN_ROLE_TITLE);
            assertThat(role.getOwnerScopeType()).isEqualTo(RoleOwnerScopeType.TENANT);
            assertThat(role.getOwnerScopeId()).isEqualTo("acme");
            assertThat(role.getOwnerScopeKey()).isEqualTo("tenant:acme");
            assertThat(role.getSystemManaged()).isTrue();
            assertThat(roleActionDao.list(Criteria.of())).hasSize(1);
        }
    }

    @Test
    void shouldRepairExistingTenantAdminRoleScopeDuringProvisioning() {
        when(grantableActionResolver.resolve(any())).thenReturn(List.of());
        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            Tenant tenant = new Tenant();
            tenant.setAlias("acme");
            tenant.setTitle("Acme");
            tenant.setEnabled(Boolean.TRUE);
            tenantService.insert(tenant);
        }
        Role legacy = new Role();
        legacy.setId(DefaultTenantRoleProvisioner.tenantAdminRoleId("acme"));
        legacy.setTenantId("acme");
        legacy.setTitle(RoleService.TENANT_ADMIN_ROLE_TITLE);
        legacy.setOwnerScopeType(RoleOwnerScopeType.TENANT);
        legacy.setOwnerScopeKey("tenant:");
        legacy.setSystemManaged(Boolean.TRUE);
        legacy.setBuiltIn(Boolean.TRUE);
        legacy.setEnabled(Boolean.TRUE);
        legacy.setVersion(0);
        roleDao.insert(legacy);

        Role repaired = tenantRoleProvisioner.ensureTenantAdminRole("acme");

        assertThat(repaired.getOwnerScopeId()).isEqualTo("acme");
        assertThat(repaired.getOwnerScopeKey()).isEqualTo("tenant:acme");
        assertThat(repaired.getAssignmentType()).isEqualTo(RoleAssignmentType.ACCOUNT);
        assertThat(roleDao.findById(repaired.getId()).getOwnerScopeId()).isEqualTo("acme");
    }

    @Test
    void shouldProvisionOrganizationAdminRoleWhenOrganizationIsCreated() {
        when(grantableActionResolver.resolve(any())).thenReturn(List.of(
                GrantableAction.ofPlatformDefaults("iam.employee", PlatformAction.QUERY)
        ));
        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            Tenant tenant = new Tenant();
            tenant.setAlias("acme");
            tenant.setTitle("Acme");
            tenant.setEnabled(Boolean.TRUE);
            tenantService.insert(tenant);
        }
        @SuppressWarnings("unchecked")
        ObjectProvider<OrganizationCreationProvisioner> provisioners = mock(ObjectProvider.class);
        when(provisioners.orderedStream()).thenAnswer(invocation -> Stream.of(organizationRoleProvisioner));
        OrganizationService provisioningOrganizationService =
                new OrganizationService(organizationDao, tenantService, Optional.empty(), provisioners);
        Organization organization = new Organization();
        organization.setId("org-1");
        organization.setCode("HQ");
        organization.setTitle("Headquarters");

        try (TenantContext.Scope ignored = TenantContext.use("acme")) {
            provisioningOrganizationService.insert(organization);
            Role role = roleService.select(DefaultOrganizationRoleProvisioner.organizationAdminRoleId("acme", "org-1"));

            assertThat(role).isNotNull();
            assertThat(role.getTitle()).isEqualTo(DefaultOrganizationRoleProvisioner.ORGANIZATION_ADMIN_ROLE_TITLE);
            assertThat(role.getAssignmentType()).isEqualTo(RoleAssignmentType.ACCOUNT);
            assertThat(role.getOwnerScopeType()).isEqualTo(RoleOwnerScopeType.ORGANIZATION);
            assertThat(role.getOwnerScopeId()).isEqualTo("org-1");
            assertThat(role.getOwnerScopeKey()).isEqualTo("organization:org-1");
            assertThat(role.getSharePolicy()).isEqualTo(RoleSharePolicy.OWNER_AND_CHILDREN);
            assertThat(role.getSystemManaged()).isTrue();
            assertThat(roleActionDao.list(Criteria.of())).hasSize(1);
        }
    }

    @Test
    void shouldGrantOrganizationAdminRoleToAccountWithOrganizationScope() {
        when(grantableActionResolver.resolve(any())).thenReturn(List.of());
        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            Tenant tenant = new Tenant();
            tenant.setAlias("acme");
            tenant.setTitle("Acme");
            tenant.setEnabled(Boolean.TRUE);
            tenantService.insert(tenant);
        }
        try (TenantContext.Scope ignored = TenantContext.use("acme")) {
            UserAccount user = new UserAccount();
            user.setId("user-1");
            user.setUsername("org_admin");
            user.setPassword("secret");
            user.setEnabled(Boolean.TRUE);
            userAccountService.insert(user);
        }

        Role role = organizationRoleProvisioner.grantOrganizationAdminRoleToUser("acme", "org-1", "user-1");

        assertThat(role.getAssignmentType()).isEqualTo(RoleAssignmentType.ACCOUNT);
        try (TenantContext.Scope ignored = TenantContext.use("acme")) {
            assertThat(accountRoleGrantDao.list(Criteria.of()))
                    .singleElement()
                    .satisfies(grant -> {
                        assertThat(grant.getRoleId()).isEqualTo(role.getId());
                        assertThat(grant.getUserId()).isEqualTo("user-1");
                        assertThat(grant.getManagementScopeType()).isEqualTo(ManagementScopeType.ORGANIZATION);
                        assertThat(grant.getManagementScopeId()).isEqualTo("org-1");
                    });
        }
    }

    @Test
    void shouldRejectNonSystemManagedRoleWhenProvisioningAdminRoleId() {
        when(grantableActionResolver.resolve(any())).thenReturn(List.of());
        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            Tenant tenant = new Tenant();
            tenant.setAlias("acme");
            tenant.setTitle("Acme");
            tenant.setEnabled(Boolean.TRUE);
            tenantService.insert(tenant);
        }
        Role existing = new Role();
        existing.setId(DefaultTenantRoleProvisioner.tenantAdminRoleId("acme"));
        existing.setTenantId("acme");
        existing.setAssignmentType(RoleAssignmentType.ACCOUNT);
        existing.setRoleKind(net.ximatai.muyun.spring.iam.role.RoleKind.STANDARD);
        existing.setTitle("业务角色");
        existing.setOwnerScopeType(RoleOwnerScopeType.TENANT);
        existing.setOwnerScopeId("acme");
        existing.setOwnerScopeKey("tenant:acme");
        existing.setSystemManaged(Boolean.FALSE);
        existing.setBuiltIn(Boolean.FALSE);
        existing.setEnabled(Boolean.TRUE);
        existing.setVersion(0);
        roleDao.insert(existing);

        assertThatThrownBy(() -> tenantRoleProvisioner.ensureTenantAdminRole("acme"))
                .isInstanceOf(net.ximatai.muyun.spring.common.exception.PlatformException.class)
                .hasMessageContaining("non system managed role");
    }

    @Test
    void shouldRestoreSoftDeletedSystemManagedAdminRoleDuringProvisioning() {
        when(grantableActionResolver.resolve(any())).thenReturn(List.of());
        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            Tenant tenant = new Tenant();
            tenant.setAlias("acme");
            tenant.setTitle("Acme");
            tenant.setEnabled(Boolean.TRUE);
            tenantService.insert(tenant);
        }
        Role deleted = new Role();
        deleted.setId(DefaultOrganizationRoleProvisioner.organizationAdminRoleId("acme", "org-1"));
        deleted.setTenantId("acme");
        deleted.setAssignmentType(RoleAssignmentType.EMPLOYMENT);
        deleted.setRoleKind(net.ximatai.muyun.spring.iam.role.RoleKind.STANDARD);
        deleted.setTitle(DefaultOrganizationRoleProvisioner.ORGANIZATION_ADMIN_ROLE_TITLE);
        deleted.setOwnerScopeType(RoleOwnerScopeType.ORGANIZATION);
        deleted.setOwnerScopeId("org-1");
        deleted.setOwnerScopeKey("organization:org-1");
        deleted.setSharePolicy(RoleSharePolicy.OWNER_AND_CHILDREN);
        deleted.setSystemManaged(Boolean.TRUE);
        deleted.setBuiltIn(Boolean.TRUE);
        deleted.setEnabled(Boolean.TRUE);
        deleted.setDeleted(Boolean.TRUE);
        deleted.setDeletedAt(Instant.parse("2026-01-01T00:00:00Z"));
        deleted.setVersion(0);
        roleDao.insert(deleted);

        Role repaired = organizationRoleProvisioner.ensureOrganizationAdminRole("acme", "org-1");

        assertThat(repaired.getAssignmentType()).isEqualTo(RoleAssignmentType.ACCOUNT);
        assertThat(repaired.getDeleted()).isFalse();
        assertThat(repaired.getDeletedAt()).isNull();
        Role persisted = roleDao.findById(repaired.getId());
        assertThat(persisted.getDeleted()).isFalse();
        assertThat(persisted.getDeletedAt()).isNull();
    }

    @Test
    void shouldCreateDemoTenantOrganizationDepartmentAndEmployeeIdempotently() {
        MuYunSpringDemoBootstrapProperties properties = new MuYunSpringDemoBootstrapProperties();
        properties.setEnabled(true);
        properties.setTenantTitle("演示租户");
        properties.setOrganizationTitle("戏码台");
        properties.setDepartmentTitle("综合管理部");
        properties.setEmployeeTitle("演示租户管理员");
        properties.setAdminUsername("demo_admin");
        properties.setAdminInitialPassword("demo123");
        when(grantableActionResolver.resolve(any())).thenReturn(List.of(
                GrantableAction.ofPlatformDefaults("iam.user", PlatformAction.MENU),
                GrantableAction.ofPlatformDefaults("iam.user", PlatformAction.QUERY)
        ));
        DemoBootstrapTask task = new DemoBootstrapTask(properties, tenantService, organizationService,
                departmentService, employeeService, userAccountService, employeeAccountService, tenantRoleProvisioner);

        task.run();
        task.run();

        assertThat(BuiltInRolePermissionTemplateService.TENANT_ADMIN_MODULE_ALIASES)
                .doesNotContain("iam.tenant");
        verify(grantableActionResolver, times(2))
                .resolve(BuiltInRolePermissionTemplateService.TENANT_ADMIN_MODULE_ALIASES);

        Tenant tenant = tenantService.select(DemoBootstrapTask.TENANT_ALIAS);
        assertThat(tenant).isNotNull();
        assertThat(tenant.getTitle()).isEqualTo("演示租户");
        assertThat(tenant.getTenantId()).isNull();

        try (TenantContext.Scope ignored = TenantContext.use(DemoBootstrapTask.TENANT_ALIAS)) {
            Organization organization = organizationService.select(DemoBootstrapTask.ORGANIZATION_ID);
            Department department = departmentService.select(DemoBootstrapTask.DEPARTMENT_ID);
            Employee employee = employeeService.select(DemoBootstrapTask.EMPLOYEE_ID);
            UserAccount user = userAccountService.select(DemoBootstrapTask.USER_ID);
            EmployeeAccount binding = employeeAccountService.select(DemoBootstrapTask.EMPLOYEE_ACCOUNT_ID);
            Role role = roleService.select(DefaultTenantRoleProvisioner.tenantAdminRoleId(DemoBootstrapTask.TENANT_ALIAS));

            assertThat(organization).isNotNull();
            assertThat(organization.getCode()).isEqualTo(DemoBootstrapTask.ORGANIZATION_CODE);
            assertThat(organization.getTitle()).isEqualTo("戏码台");

            assertThat(department).isNotNull();
            assertThat(department.getOrganizationId()).isEqualTo(DemoBootstrapTask.ORGANIZATION_ID);
            assertThat(department.getCode()).isEqualTo(DemoBootstrapTask.DEPARTMENT_CODE);
            assertThat(department.getTitle()).isEqualTo("综合管理部");

            assertThat(employee).isNotNull();
            assertThat(employee.getOrganizationId()).isEqualTo(DemoBootstrapTask.ORGANIZATION_ID);
            assertThat(employee.getDepartmentId()).isEqualTo(DemoBootstrapTask.DEPARTMENT_ID);
            assertThat(employee.getEmployeeNo()).isEqualTo(DemoBootstrapTask.EMPLOYEE_NO);
            assertThat(employee.getTitle()).isEqualTo("演示租户管理员");

            assertThat(user).isNotNull();
            assertThat(user.getUsername()).isEqualTo("demo_admin");
            assertThat(user.getOrganizationId()).isEqualTo(DemoBootstrapTask.ORGANIZATION_ID);
            assertThat(user.getTenantId()).isEqualTo(DemoBootstrapTask.TENANT_ALIAS);
            assertThat(userAccountService.passwordMatches(user, "demo123")).isTrue();

            assertThat(binding).isNotNull();
            assertThat(binding.getEmployeeId()).isEqualTo(DemoBootstrapTask.EMPLOYEE_ID);
            assertThat(binding.getUserId()).isEqualTo(DemoBootstrapTask.USER_ID);
            assertThat(binding.getPrimaryAccount()).isTrue();

            assertThat(role).isNotNull();
            assertThat(role.getTitle()).isEqualTo(RoleService.TENANT_ADMIN_ROLE_TITLE);
            assertThat(role.getOwnerScopeType()).isEqualTo(RoleOwnerScopeType.TENANT);
            assertThat(role.getOwnerScopeId()).isEqualTo(DemoBootstrapTask.TENANT_ALIAS);
            assertThat(role.getOwnerScopeKey()).isEqualTo("tenant:" + DemoBootstrapTask.TENANT_ALIAS);
            assertThat(roleService.hasActionPermission(DemoBootstrapTask.USER_ID, "iam.user",
                    PlatformAction.MENU.code())).isTrue();
            assertThat(roleService.hasActionPermission(DemoBootstrapTask.USER_ID, "iam.user",
                    PlatformAction.QUERY.code())).isTrue();
            assertThat(roleService.hasActionPermission(DemoBootstrapTask.USER_ID, "iam.tenant",
                    PlatformAction.QUERY.code())).isFalse();
        }

        assertThat(tenantDao.list(Criteria.of())).hasSize(1);
        try (TenantContext.Scope ignored = TenantContext.use(DemoBootstrapTask.TENANT_ALIAS)) {
            assertThat(organizationDao.list(Criteria.of())).hasSize(1);
            assertThat(departmentDao.list(Criteria.of())).hasSize(1);
            assertThat(employeeDao.list(Criteria.of())).hasSize(1);
            assertThat(userAccountDao.list(Criteria.of())).hasSize(1);
            assertThat(employeeAccountDao.list(Criteria.of())).hasSize(1);
            assertThat(roleDao.list(Criteria.of())).hasSize(1);
            assertThat(accountRoleGrantDao.list(Criteria.of()))
                    .singleElement()
                    .satisfies(grant -> {
                        assertThat(grant.getRoleId()).isEqualTo(
                                DefaultTenantRoleProvisioner.tenantAdminRoleId(DemoBootstrapTask.TENANT_ALIAS));
                        assertThat(grant.getUserId()).isEqualTo(DemoBootstrapTask.USER_ID);
                        assertThat(grant.getManagementScopeType()).isEqualTo(ManagementScopeType.TENANT);
                        assertThat(grant.getManagementScopeId()).isEqualTo(DemoBootstrapTask.TENANT_ALIAS);
                    });
            assertThat(employmentRoleGrantDao.list(Criteria.of())).isEmpty();
            assertThat(roleActionDao.list(Criteria.of())).hasSize(2);
            Optional<RoleAction> queryGrant = roleActionDao.list(Criteria.of()).stream()
                    .filter(action -> PlatformAction.QUERY.permissionActionCode().equals(action.getActionCode()))
                    .findFirst();
            assertThat(queryGrant).hasValueSatisfying(action -> {
                assertThat(action.getDataScopePolicy()).isEqualTo(DataScopePolicy.NONE);
                assertThat(action.getTenantScopePolicy()).isEqualTo(TenantScopePolicy.CURRENT_TENANT);
            });
        }
    }

    @Test
    void shouldAllowExistingDemoAdminPasswordRotation() {
        MuYunSpringDemoBootstrapProperties properties = new MuYunSpringDemoBootstrapProperties();
        properties.setEnabled(true);
        properties.setAdminInitialPassword("demo123");
        when(grantableActionResolver.resolve(any())).thenReturn(List.of());
        DemoBootstrapTask task = new DemoBootstrapTask(properties, tenantService, organizationService,
                departmentService, employeeService, userAccountService, employeeAccountService, tenantRoleProvisioner);

        task.run();
        try (TenantContext.Scope ignored = TenantContext.use(DemoBootstrapTask.TENANT_ALIAS)) {
            assertThat(userAccountService.changePassword(DemoBootstrapTask.USER_ID, "rotated123")).isEqualTo(1);
        }

        task.run();

        try (TenantContext.Scope ignored = TenantContext.use(DemoBootstrapTask.TENANT_ALIAS)) {
            UserAccount user = userAccountService.select(DemoBootstrapTask.USER_ID);
            assertThat(userAccountService.passwordMatches(user, "rotated123")).isTrue();
            assertThat(userAccountService.passwordMatches(user, "demo123")).isFalse();
        }
    }

    @Test
    void shouldReplayTenantProvisioningWhenDemoTenantAlreadyExists() {
        MuYunSpringDemoBootstrapProperties properties = new MuYunSpringDemoBootstrapProperties();
        properties.setEnabled(true);
        when(grantableActionResolver.resolve(any())).thenReturn(List.of());
        TenantService replayingTenantService = spy(new TenantService(tenantDao));
        DemoBootstrapTask task = new DemoBootstrapTask(properties, replayingTenantService, organizationService,
                departmentService, employeeService, userAccountService, employeeAccountService, tenantRoleProvisioner);

        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            Tenant tenant = new Tenant();
            tenant.setAlias(DemoBootstrapTask.TENANT_ALIAS);
            tenant.setTitle("演示租户");
            tenant.setEnabled(Boolean.TRUE);
            replayingTenantService.insert(tenant);
        }
        clearInvocations(replayingTenantService);

        task.run();

        verify(replayingTenantService).provisionTenant(DemoBootstrapTask.TENANT_ALIAS);
    }

    @Test
    void shouldFailFastWhenExistingDemoAdminUserDrifts() {
        MuYunSpringDemoBootstrapProperties properties = new MuYunSpringDemoBootstrapProperties();
        properties.setEnabled(true);
        when(grantableActionResolver.resolve(any())).thenReturn(List.of());
        DemoBootstrapTask task = new DemoBootstrapTask(properties, tenantService, organizationService,
                departmentService, employeeService, userAccountService, employeeAccountService, tenantRoleProvisioner);

        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            Tenant tenant = new Tenant();
            tenant.setAlias(DemoBootstrapTask.TENANT_ALIAS);
            tenant.setTitle("演示租户");
            tenant.setEnabled(Boolean.TRUE);
            tenantService.insert(tenant);
        }
        try (TenantContext.Scope ignored = TenantContext.use(DemoBootstrapTask.TENANT_ALIAS)) {
            Organization organization = new Organization();
            organization.setId(DemoBootstrapTask.ORGANIZATION_ID);
            organization.setCode(DemoBootstrapTask.ORGANIZATION_CODE);
            organization.setTitle("戏码台");
            organization.setEnabled(Boolean.TRUE);
            organizationService.insert(organization);

            Department department = new Department();
            department.setId(DemoBootstrapTask.DEPARTMENT_ID);
            department.setOrganizationId(DemoBootstrapTask.ORGANIZATION_ID);
            department.setCode(DemoBootstrapTask.DEPARTMENT_CODE);
            department.setTitle("综合管理部");
            department.setEnabled(Boolean.TRUE);
            departmentService.insert(department);

            Employee employee = new Employee();
            employee.setId(DemoBootstrapTask.EMPLOYEE_ID);
            employee.setOrganizationId(DemoBootstrapTask.ORGANIZATION_ID);
            employee.setDepartmentId(DemoBootstrapTask.DEPARTMENT_ID);
            employee.setEmployeeNo(DemoBootstrapTask.EMPLOYEE_NO);
            employee.setTitle("演示租户管理员");
            employee.setEnabled(Boolean.TRUE);
            employeeService.insert(employee);

            UserAccount user = new UserAccount();
            user.setId(DemoBootstrapTask.USER_ID);
            user.setUsername("other_admin");
            user.setPassword("demo123");
            user.setOrganizationId(DemoBootstrapTask.ORGANIZATION_ID);
            user.setEnabled(Boolean.TRUE);
            userAccountService.insert(user);
        }

        assertThatThrownBy(task::run)
                .isInstanceOf(net.ximatai.muyun.spring.common.exception.PlatformException.class)
                .hasMessageContaining("demo admin username drift");
    }

    private static class TenantMemoryDao extends TestMemoryDao<Tenant> implements TenantDao {
    }

    private static class OrganizationMemoryDao extends TestMemoryDao<Organization> implements OrganizationDao {
    }

    private static class DepartmentMemoryDao extends TestMemoryDao<Department> implements DepartmentDao {
    }

    private static class EmployeeMemoryDao extends TestMemoryDao<Employee> implements EmployeeDao {
    }

    private static class UserAccountMemoryDao extends TestMemoryDao<UserAccount> implements UserAccountDao {
    }

    private static class EmployeeAccountMemoryDao extends TestMemoryDao<EmployeeAccount> implements EmployeeAccountDao {
    }

    private static class RoleMemoryDao extends TestMemoryDao<Role> implements RoleDao {
    }

    private static class AccountRoleGrantMemoryDao extends TestMemoryDao<AccountRoleGrant>
            implements AccountRoleGrantDao {
    }

    private static class EmploymentRoleGrantMemoryDao extends TestMemoryDao<EmploymentRoleGrant>
            implements EmploymentRoleGrantDao {
    }

    private static class RoleActionMemoryDao extends TestMemoryDao<RoleAction> implements RoleActionDao {
    }
}
