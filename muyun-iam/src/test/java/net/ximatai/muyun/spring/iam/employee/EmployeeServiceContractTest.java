package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmployeeServiceContractTest {
    @Test
    void shouldExposeStableModuleAlias() {
        EmployeeService service = new EmployeeService(mock(EmployeeDao.class), activeTenantVerifier(),
                organizationService(), departmentService());

        assertThat(service.getModuleAlias()).isEqualTo("iam.employee");
    }

    @Test
    void shouldFillEmployeeDefaultsThroughCrudAbility() {
        EmployeeDao dao = mock(EmployeeDao.class);
        when(dao.insert(any())).thenReturn("employee-1");
        OrganizationService organizationService = organizationService();
        DepartmentService departmentService = departmentService();
        when(organizationService.requireEnabled(eq("org-1"), any())).thenReturn(organization("org-1"));
        when(departmentService.requireEnabled(eq("dept-1"), any())).thenReturn(department("org-1", "dept-1"));
        EmployeeService service = new EmployeeService(dao, activeTenantVerifier(), organizationService,
                departmentService);
        Employee employee = employee("org-1", "dept-1", "E001", "Alice");

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            service.insert(employee);
        }

        assertThat(employee.getEnabled()).isTrue();
        assertThat(employee.getTenantId()).isEqualTo("tenant_a");
        assertThat(employee.getMobile()).isNull();
        verify(organizationService).requireEnabled(eq("org-1"), any());
        verify(departmentService).requireEnabled(eq("dept-1"), any());
    }

    @Test
    void shouldRequireTenantContextForEmployeeMutation() {
        EmployeeService service = new EmployeeService(mock(EmployeeDao.class), activeTenantVerifier(),
                organizationService(), departmentService());

        assertThatThrownBy(() -> service.insert(employee("org-1", "dept-1", "E001", "Alice")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("tenant context");
    }

    @Test
    void shouldRequireCoreEmployeeFields() {
        EmployeeService service = new EmployeeService(mock(EmployeeDao.class), activeTenantVerifier(),
                organizationService(), departmentService());

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(employee(" ", "dept-1", "E001", "Alice")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("organizationId");
            assertThatThrownBy(() -> service.insert(employee("org-1", " ", "E001", "Alice")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("departmentId");
            assertThatThrownBy(() -> service.insert(employee("org-1", "dept-1", " ", "Alice")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employeeNo");
            assertThatThrownBy(() -> service.insert(employee("org-1", "dept-1", "E001", " ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("employeeName");
        }
    }

    @Test
    void shouldRejectDepartmentFromAnotherOrganization() {
        OrganizationService organizationService = organizationService();
        DepartmentService departmentService = departmentService();
        when(organizationService.requireEnabled(eq("org-1"), any())).thenReturn(organization("org-1"));
        when(departmentService.requireEnabled(eq("dept-2"), any())).thenReturn(department("org-2", "dept-2"));
        EmployeeService service = new EmployeeService(mock(EmployeeDao.class), activeTenantVerifier(),
                organizationService, departmentService);

        try (TenantContext.Scope ignored = TenantContext.use("tenant_a")) {
            assertThatThrownBy(() -> service.insert(employee("org-1", "dept-2", "E001", "Alice")))
                    .isInstanceOf(PlatformException.class)
                    .hasMessageContaining("same organization");
        }
    }

    @Test
    void shouldSortOnlyWithinDepartment() {
        EmployeeService service = new EmployeeService(mock(EmployeeDao.class), activeTenantVerifier(),
                organizationService(), departmentService());

        assertThatThrownBy(() -> service.validateSortScope(
                employee("org-1", "dept-1", "E001", "Alice"),
                employee("org-1", "dept-2", "E002", "Bob")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("same department");
    }

    private Employee employee(String organizationId, String departmentId, String employeeNo, String title) {
        Employee employee = new Employee();
        employee.setOrganizationId(organizationId);
        employee.setDepartmentId(departmentId);
        employee.setEmployeeNo(employeeNo);
        employee.setTitle(title);
        employee.setMobile(" ");
        return employee;
    }

    private ActiveTenantVerifier activeTenantVerifier() {
        return mock(ActiveTenantVerifier.class);
    }

    private OrganizationService organizationService() {
        return mock(OrganizationService.class);
    }

    private DepartmentService departmentService() {
        return mock(DepartmentService.class);
    }

    private Organization organization(String id) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setEnabled(Boolean.TRUE);
        return organization;
    }

    private Department department(String organizationId, String id) {
        Department department = new Department();
        department.setId(id);
        department.setOrganizationId(organizationId);
        department.setEnabled(Boolean.TRUE);
        return department;
    }
}
