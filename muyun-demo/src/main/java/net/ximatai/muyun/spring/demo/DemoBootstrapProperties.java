package net.ximatai.muyun.spring.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("muyun.demo-bootstrap")
public class DemoBootstrapProperties {
    private String tenantTitle = "演示租户";
    private String organizationTitle = "戏码台";
    private String departmentTitle = "综合管理部";
    private String employeeTitle = "演示租户管理员";
    private String adminUsername = "demo_admin";
    private String adminInitialPassword = "demo123";

    public String getTenantTitle() {
        return tenantTitle;
    }

    public void setTenantTitle(String tenantTitle) {
        this.tenantTitle = requireText(tenantTitle, "muyun.demo-bootstrap.tenant-title");
    }

    public String getOrganizationTitle() {
        return organizationTitle;
    }

    public void setOrganizationTitle(String organizationTitle) {
        this.organizationTitle = requireText(organizationTitle, "muyun.demo-bootstrap.organization-title");
    }

    public String getDepartmentTitle() {
        return departmentTitle;
    }

    public void setDepartmentTitle(String departmentTitle) {
        this.departmentTitle = requireText(departmentTitle, "muyun.demo-bootstrap.department-title");
    }

    public String getEmployeeTitle() {
        return employeeTitle;
    }

    public void setEmployeeTitle(String employeeTitle) {
        this.employeeTitle = requireText(employeeTitle, "muyun.demo-bootstrap.employee-title");
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = requireText(adminUsername, "muyun.demo-bootstrap.admin-username");
    }

    public String getAdminInitialPassword() {
        return adminInitialPassword;
    }

    public void setAdminInitialPassword(String adminInitialPassword) {
        this.adminInitialPassword = requireText(adminInitialPassword, "muyun.demo-bootstrap.admin-initial-password");
    }

    private String requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(propertyName + " must not be blank");
        }
        return value.trim();
    }
}
