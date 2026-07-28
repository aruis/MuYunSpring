package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeePosition;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.position.PositionService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmployeeEmploymentReadServiceTest {
    @Test
    void shouldKeepRetainedEmployeeFieldsInRecycleBinEmploymentProjection() {
        EmployeePositionService employeePositionService = mock(EmployeePositionService.class);
        EmployeeService employeeService = mock(EmployeeService.class);
        OrganizationService organizationService = mock(OrganizationService.class);
        DepartmentService departmentService = mock(DepartmentService.class);
        PositionService positionService = mock(PositionService.class);
        EmployeeAccountService employeeAccountService = mock(EmployeeAccountService.class);
        UserAccountService userAccountService = mock(UserAccountService.class);
        EmployeeEmploymentReadService readService = new EmployeeEmploymentReadService(
                employeePositionService, employeeService, organizationService, departmentService,
                positionService, employeeAccountService, userAccountService);

        Employee retained = new Employee();
        retained.setId("employee-1");
        retained.setEmployeeNo("E001");
        retained.setTitle("测试职员");
        retained.setDeleted(true);
        EmployeePosition employment = new EmployeePosition();
        employment.setId("employment-1");
        employment.setEmployeeId("employee-1");
        employment.setOrganizationId("org-1");
        employment.setDepartmentId("dept-1");
        employment.setPositionId("position-1");
        PageRequest page = PageRequest.of(1, 20);

        when(employeePositionService.pageQuery(any(), any(), any()))
                .thenReturn(PageResult.of(List.of(employment), 1, page));
        when(employeeService.list(any(), any(PageRequest.class))).thenReturn(List.of());
        when(organizationService.list(any(), any(PageRequest.class))).thenReturn(List.of());
        when(departmentService.list(any(), any(PageRequest.class))).thenReturn(List.of());
        when(positionService.list(any(), any(PageRequest.class))).thenReturn(List.of());
        when(employeeAccountService.list(any(), any(PageRequest.class))).thenReturn(List.of());
        when(userAccountService.list(any(), any(PageRequest.class))).thenReturn(List.of());

        var result = readService.pageForEmployee(retained,
                new EmployeeEmploymentReadService.Query("employee-1", null, null, false, page));

        assertThat(result.getRecords()).singleElement().satisfies(view -> {
            assertThat(view.employeeNo()).isEqualTo("E001");
            assertThat(view.employeeTitle()).isEqualTo("测试职员");
        });
    }
}
