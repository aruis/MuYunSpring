package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.initialdata.InitialDataAbility;
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
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantDao;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.PasswordHashingService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountDao;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.application.ApplicationDao;
import net.ximatai.muyun.spring.platform.application.ApplicationService;
import net.ximatai.muyun.spring.platform.initialdata.InitialDataExecutor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformBaselineInitialDataTest {
    private final ApplicationMemoryDao applicationDao = new ApplicationMemoryDao();
    private final TenantMemoryDao tenantDao = new TenantMemoryDao();
    private final OrganizationMemoryDao organizationDao = new OrganizationMemoryDao();
    private final DepartmentMemoryDao departmentDao = new DepartmentMemoryDao();
    private final EmployeeMemoryDao employeeDao = new EmployeeMemoryDao();
    private final UserAccountMemoryDao userAccountDao = new UserAccountMemoryDao();
    private final EmployeeAccountMemoryDao employeeAccountDao = new EmployeeAccountMemoryDao();

    private final ApplicationService applicationService = new ApplicationService(applicationDao);
    private final TenantService tenantService = new TenantService(tenantDao);
    private final OrganizationService organizationService = new OrganizationService(organizationDao, tenantService);
    private final DepartmentService departmentService = new DepartmentService(
            departmentDao, tenantService, organizationService);
    private final EmployeeService employeeService = new EmployeeService(
            employeeDao, tenantService, organizationService, departmentService);
    private final UserAccountService userAccountService = new UserAccountService(
            userAccountDao, tenantService, new PasswordHashingService());
    private final EmployeeAccountService employeeAccountService = new EmployeeAccountService(
            employeeAccountDao, tenantService, employeeService, userAccountService);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldInitializePlatformBaselineDataIdempotently() {
        initializeBaseline();
        initializeBaseline();

        assertThat(applicationDao.list(Criteria.of()))
                .extracting(Application::getAlias)
                .contains("platform", "iam");
        assertThat(tenantService.select(TenantService.PLATFORM_TENANT_ID))
                .extracting(Tenant::getTitle)
                .isEqualTo(TenantService.PLATFORM_TENANT_TITLE);

        try (TenantContext.Scope ignored = TenantContext.use(TenantService.PLATFORM_TENANT_ID)) {
            Organization organization = organizationService.select(OrganizationService.PLATFORM_ROOT_ORGANIZATION_ID);
            assertThat(organization).isNotNull();
            assertThat(organization.getCode()).isEqualTo(OrganizationService.PLATFORM_ROOT_ORGANIZATION_CODE);

            Department department = departmentService.select(DepartmentService.PLATFORM_ROOT_DEPARTMENT_ID);
            assertThat(department).isNotNull();
            assertThat(department.getOrganizationId()).isEqualTo(organization.getId());

            Employee employee = employeeService.select(EmployeeService.PLATFORM_ADMIN_EMPLOYEE_ID);
            assertThat(employee).isNotNull();
            assertThat(employee.getOrganizationId()).isEqualTo(organization.getId());
            assertThat(employee.getDepartmentId()).isEqualTo(department.getId());

            UserAccount user = userAccountService.select(UserAccountService.PLATFORM_SUPER_ADMIN_USER_ID);
            assertThat(user).isNotNull();
            assertThat(user.getOrganizationId()).isEqualTo(organization.getId());
            assertThat(userAccountService.passwordMatches(user, "admin123")).isTrue();

            assertThat(employeeAccountDao.list(Criteria.of()))
                    .singleElement()
                    .satisfies(binding -> {
                        assertThat(binding.getEmployeeId()).isEqualTo(employee.getId());
                        assertThat(binding.getUserId()).isEqualTo(user.getId());
                        assertThat(binding.getPrimaryAccount()).isTrue();
                        assertThat(binding.getEnabled()).isTrue();
                    });
        }
    }

    private void initializeBaseline() {
        new InitialDataExecutor(
                List.<InitialDataAbility<?>>of(
                        applicationService,
                        tenantService,
                        organizationService,
                        departmentService,
                        employeeService,
                        userAccountService,
                        employeeAccountService
                ),
                List.of()
        ).initializeAll();
    }

    private static class ApplicationMemoryDao extends TestMemoryDao<Application> implements ApplicationDao {
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
}
