package net.ximatai.muyun.spring.demo;

import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.role.DefaultTenantRoleProvisioner;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Declares the optional demo-data bootstrap owned by the demo module. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DemoBootstrapProperties.class)
public class DemoBootstrapConfiguration {
    @Bean
    DemoBootstrapTask demoBootstrapTask(DemoBootstrapProperties properties, TenantService tenantService,
                                        OrganizationService organizationService, DepartmentService departmentService,
                                        EmployeeService employeeService, UserAccountService userAccountService,
                                        EmployeeAccountService employeeAccountService,
                                        DefaultTenantRoleProvisioner tenantRoleProvisioner) {
        return new DemoBootstrapTask(properties, tenantService, organizationService, departmentService,
                employeeService, userAccountService, employeeAccountService, tenantRoleProvisioner);
    }
}
