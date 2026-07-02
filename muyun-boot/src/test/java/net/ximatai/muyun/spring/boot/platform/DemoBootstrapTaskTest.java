package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.MuYunSpringDemoBootstrapProperties;
import net.ximatai.muyun.spring.boot.iam.RoleGrantableActionResolver;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
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
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RoleAction;
import net.ximatai.muyun.spring.iam.role.RoleActionDao;
import net.ximatai.muyun.spring.iam.role.RoleDao;
import net.ximatai.muyun.spring.iam.role.RoleGrant;
import net.ximatai.muyun.spring.iam.role.RoleGrantDao;
import net.ximatai.muyun.spring.iam.role.RoleGrantSubjectType;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantDao;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.PasswordHashingService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountDao;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DemoBootstrapTaskTest {
    private final TenantMemoryDao tenantDao = new TenantMemoryDao();
    private final OrganizationMemoryDao organizationDao = new OrganizationMemoryDao();
    private final DepartmentMemoryDao departmentDao = new DepartmentMemoryDao();
    private final EmployeeMemoryDao employeeDao = new EmployeeMemoryDao();
    private final UserAccountMemoryDao userAccountDao = new UserAccountMemoryDao();
    private final EmployeeAccountMemoryDao employeeAccountDao = new EmployeeAccountMemoryDao();
    private final RoleMemoryDao roleDao = new RoleMemoryDao();
    private final RoleGrantMemoryDao roleGrantDao = new RoleGrantMemoryDao();
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
    private final RoleService roleService = new RoleService(roleDao, roleGrantDao, roleActionDao, tenantService,
            net.ximatai.muyun.spring.iam.role.RoleActionGrantVerifier.platformActionsOnly(),
            userAccountService, employeeService, null, employeeAccountService);
    private final RoleGrantableActionResolver grantableActionResolver = mock(RoleGrantableActionResolver.class);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldDoNothingWhenDemoBootstrapIsDisabled() {
        DemoBootstrapTask task = new DemoBootstrapTask(new MuYunSpringDemoBootstrapProperties(),
                tenantService, organizationService, departmentService, employeeService, userAccountService,
                employeeAccountService, roleService, grantableActionResolver);

        task.run();

        assertThat(tenantDao.list(Criteria.of())).isEmpty();
        assertThat(organizationDao.list(Criteria.of())).isEmpty();
        assertThat(departmentDao.list(Criteria.of())).isEmpty();
        assertThat(employeeDao.list(Criteria.of())).isEmpty();
        assertThat(userAccountDao.list(Criteria.of())).isEmpty();
        assertThat(roleDao.list(Criteria.of())).isEmpty();
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
                departmentService, employeeService, userAccountService, employeeAccountService, roleService,
                grantableActionResolver);

        task.run();
        task.run();

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
            Role role = roleService.select(DemoBootstrapTask.TENANT_ADMIN_ROLE_ID);

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
            assertThat(role.getTitle()).isEqualTo(DemoBootstrapTask.TENANT_ADMIN_ROLE_TITLE);
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
            assertThat(roleGrantDao.list(Criteria.of()))
                    .singleElement()
                    .satisfies(grant -> {
                        assertThat(grant.getRoleId()).isEqualTo(DemoBootstrapTask.TENANT_ADMIN_ROLE_ID);
                        assertThat(grant.getSubjectType()).isEqualTo(RoleGrantSubjectType.USER_ACCOUNT);
                        assertThat(grant.getSubjectId()).isEqualTo(DemoBootstrapTask.USER_ID);
                    });
            assertThat(roleActionDao.list(Criteria.of())).hasSize(2);
            Optional<RoleAction> queryGrant = roleActionDao.list(Criteria.of()).stream()
                    .filter(action -> PlatformAction.QUERY.permissionActionCode().equals(action.getActionCode()))
                    .findFirst();
            assertThat(queryGrant).hasValueSatisfying(action -> {
                assertThat(action.getDataScopePolicy()).isEqualTo(DataScopePolicy.ALL);
                assertThat(action.getTenantScopePolicy()).isEqualTo(TenantScopePolicy.CURRENT_TENANT);
            });
        }
    }

    @Test
    void shouldFailFastWhenExistingDemoAdminUserDrifts() {
        MuYunSpringDemoBootstrapProperties properties = new MuYunSpringDemoBootstrapProperties();
        properties.setEnabled(true);
        when(grantableActionResolver.resolve(any())).thenReturn(List.of());
        DemoBootstrapTask task = new DemoBootstrapTask(properties, tenantService, organizationService,
                departmentService, employeeService, userAccountService, employeeAccountService, roleService,
                grantableActionResolver);

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

    private static class RoleGrantMemoryDao extends TestMemoryDao<RoleGrant> implements RoleGrantDao {
    }

    private static class RoleActionMemoryDao extends TestMemoryDao<RoleAction> implements RoleActionDao {
    }
}
