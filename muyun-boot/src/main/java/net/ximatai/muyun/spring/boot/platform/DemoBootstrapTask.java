package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.boot.MuYunSpringDemoBootstrapProperties;
import net.ximatai.muyun.spring.boot.iam.RoleGrantableActionResolver;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.iam.department.Department;
import net.ximatai.muyun.spring.iam.department.DepartmentService;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.role.DataScopePolicy;
import net.ximatai.muyun.spring.iam.role.GrantableAction;
import net.ximatai.muyun.spring.iam.role.Role;
import net.ximatai.muyun.spring.iam.role.RoleGrantSubjectType;
import net.ximatai.muyun.spring.iam.role.RoleKind;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.role.TenantScopePolicy;
import net.ximatai.muyun.spring.iam.tenant.Tenant;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;

import java.util.List;
import java.util.Objects;

public class DemoBootstrapTask implements PlatformBootstrapTask {
    public static final String TENANT_ALIAS = "demo";
    public static final String ORGANIZATION_ID = "demo_org";
    public static final String ORGANIZATION_CODE = "DEMO";
    public static final String DEPARTMENT_ID = "demo_department_general";
    public static final String DEPARTMENT_CODE = "GENERAL";
    public static final String EMPLOYEE_ID = "demo_employee_admin";
    public static final String EMPLOYEE_NO = "DEMO-ADMIN";
    public static final String USER_ID = "demo_user_admin";
    public static final String EMPLOYEE_ACCOUNT_ID = "demo_employee_account_admin";
    public static final String TENANT_ADMIN_ROLE_ID = "demo_role_tenant_admin";
    public static final String TENANT_ADMIN_ROLE_TITLE = "租户管理员";
    private static final String SYSTEM_OPERATOR_ID = "demo-bootstrap";

    private final MuYunSpringDemoBootstrapProperties properties;
    private final TenantService tenantService;
    private final OrganizationService organizationService;
    private final DepartmentService departmentService;
    private final EmployeeService employeeService;
    private final UserAccountService userAccountService;
    private final EmployeeAccountService employeeAccountService;
    private final RoleService roleService;
    private final PlatformModuleService moduleService;
    private final RoleGrantableActionResolver grantableActionResolver;

    public DemoBootstrapTask(MuYunSpringDemoBootstrapProperties properties,
                             TenantService tenantService,
                             OrganizationService organizationService,
                             DepartmentService departmentService,
                             EmployeeService employeeService,
                             UserAccountService userAccountService,
                             EmployeeAccountService employeeAccountService,
                             RoleService roleService,
                             PlatformModuleService moduleService,
                             RoleGrantableActionResolver grantableActionResolver) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.tenantService = Objects.requireNonNull(tenantService, "tenantService must not be null");
        this.organizationService = Objects.requireNonNull(organizationService, "organizationService must not be null");
        this.departmentService = Objects.requireNonNull(departmentService, "departmentService must not be null");
        this.employeeService = Objects.requireNonNull(employeeService, "employeeService must not be null");
        this.userAccountService = Objects.requireNonNull(userAccountService, "userAccountService must not be null");
        this.employeeAccountService = Objects.requireNonNull(employeeAccountService,
                "employeeAccountService must not be null");
        this.roleService = Objects.requireNonNull(roleService, "roleService must not be null");
        this.moduleService = Objects.requireNonNull(moduleService, "moduleService must not be null");
        this.grantableActionResolver = Objects.requireNonNull(grantableActionResolver,
                "grantableActionResolver must not be null");
    }

    @Override
    public String name() {
        return "demo-bootstrap";
    }

    @Override
    public int order() {
        return 200;
    }

    @Override
    public void run() {
        if (!properties.isEnabled()) {
            return;
        }
        try (CurrentUserContext.Scope ignoredUser = CurrentUserContext.use(
                CurrentUser.systemUser(SYSTEM_OPERATOR_ID, "Demo Bootstrap"))) {
            Tenant tenant = ensureTenant();
            if (!isActive(tenant)) {
                return;
            }
            try (TenantContext.Scope ignoredTenant = TenantContext.use(TENANT_ALIAS)) {
                Organization organization = ensureOrganization();
                if (!isActive(organization)) {
                    return;
                }
                Department department = ensureDepartment();
                if (!isActive(department)) {
                    return;
                }
                Employee employee = ensureEmployee();
                if (!isActive(employee)) {
                    return;
                }
                UserAccount user = ensureTenantAdminUser();
                if (!isActive(user)) {
                    return;
                }
                ensureEmployeeAccount();
                Role role = ensureTenantAdminRole();
                if (!isActive(role)) {
                    return;
                }
                ensureTenantAdminRoleGrant(role.getId(), user.getId());
                ensureTenantAdminRoleActions(role.getId());
            }
        }
    }

    private Tenant ensureTenant() {
        Tenant existing = tenantService.selectIgnoreSoftDelete(TENANT_ALIAS);
        if (existing != null) {
            return existing;
        }
        Tenant tenant = new Tenant();
        tenant.setAlias(TENANT_ALIAS);
        tenant.setTitle(properties.getTenantTitle());
        tenant.setEnabled(Boolean.TRUE);
        tenant.setSortOrder(1);
        try (TenantContext.Scope ignored = TenantContext.system("demo bootstrap tenant")) {
            tenantService.insert(tenant);
        }
        return tenant;
    }

    private Organization ensureOrganization() {
        Organization existing = organizationService.selectIgnoreSoftDelete(ORGANIZATION_ID);
        if (existing != null) {
            return existing;
        }
        Organization organization = new Organization();
        organization.setId(ORGANIZATION_ID);
        organization.setCode(ORGANIZATION_CODE);
        organization.setTitle(properties.getOrganizationTitle());
        organization.setEnabled(Boolean.TRUE);
        organization.setSortOrder(1);
        organizationService.insert(organization);
        return organization;
    }

    private Department ensureDepartment() {
        Department existing = departmentService.selectIgnoreSoftDelete(DEPARTMENT_ID);
        if (existing != null) {
            return existing;
        }
        Department department = new Department();
        department.setId(DEPARTMENT_ID);
        department.setOrganizationId(ORGANIZATION_ID);
        department.setCode(DEPARTMENT_CODE);
        department.setTitle(properties.getDepartmentTitle());
        department.setEnabled(Boolean.TRUE);
        department.setSortOrder(1);
        departmentService.insert(department);
        return department;
    }

    private Employee ensureEmployee() {
        Employee existing = employeeService.selectIgnoreSoftDelete(EMPLOYEE_ID);
        if (existing != null) {
            return existing;
        }
        Employee employee = new Employee();
        employee.setId(EMPLOYEE_ID);
        employee.setOrganizationId(ORGANIZATION_ID);
        employee.setDepartmentId(DEPARTMENT_ID);
        employee.setEmployeeNo(EMPLOYEE_NO);
        employee.setTitle(properties.getEmployeeTitle());
        employee.setEnabled(Boolean.TRUE);
        employee.setSortOrder(1);
        employeeService.insert(employee);
        return employee;
    }

    private UserAccount ensureTenantAdminUser() {
        UserAccount existing = userAccountService.selectIgnoreSoftDelete(USER_ID);
        if (existing != null) {
            return existing;
        }
        UserAccount user = new UserAccount();
        user.setId(USER_ID);
        user.setUsername(properties.getAdminUsername());
        user.setPassword(properties.getAdminInitialPassword());
        user.setOrganizationId(ORGANIZATION_ID);
        user.setTitle(properties.getEmployeeTitle());
        user.setEnabled(Boolean.TRUE);
        user.setSortOrder(1);
        userAccountService.insert(user);
        return user;
    }

    private EmployeeAccount ensureEmployeeAccount() {
        EmployeeAccount existing = employeeAccountService.select(EMPLOYEE_ACCOUNT_ID);
        if (existing != null) {
            return existing;
        }
        EmployeeAccount binding = new EmployeeAccount();
        binding.setId(EMPLOYEE_ACCOUNT_ID);
        binding.setEmployeeId(EMPLOYEE_ID);
        binding.setUserId(USER_ID);
        binding.setPrimaryAccount(Boolean.TRUE);
        binding.setEnabled(Boolean.TRUE);
        employeeAccountService.insert(binding);
        return binding;
    }

    private Role ensureTenantAdminRole() {
        Role existing = roleService.selectIgnoreSoftDelete(TENANT_ADMIN_ROLE_ID);
        if (existing != null) {
            return existing;
        }
        Role role = new Role();
        role.setId(TENANT_ADMIN_ROLE_ID);
        role.setRoleKind(RoleKind.STANDARD);
        role.setGrantSubjectTypes(RoleGrantSubjectType.USER_ACCOUNT.getCode());
        role.setTitle(TENANT_ADMIN_ROLE_TITLE);
        role.setPublicRole(Boolean.FALSE);
        role.setBuiltIn(Boolean.TRUE);
        role.setSystemManaged(Boolean.TRUE);
        role.setDescription("演示租户内置管理员角色，拥有当前租户内平台可授权动作和全部数据范围。");
        role.setEnabled(Boolean.TRUE);
        role.setSortOrder(1);
        roleService.insert(role);
        return role;
    }

    private void ensureTenantAdminRoleGrant(String roleId, String userId) {
        roleService.grantRole(roleId, RoleGrantSubjectType.USER_ACCOUNT, userId);
    }

    private void ensureTenantAdminRoleActions(String roleId) {
        List<String> moduleAliases = moduleService.listSystemManagedStaticModules().stream()
                .filter(module -> module != null && Boolean.TRUE.equals(module.getEnabled()))
                .map(net.ximatai.muyun.spring.platform.module.PlatformModule::getAlias)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        for (GrantableAction action : grantableActionResolver.resolve(moduleAliases)) {
            roleService.grantAction(
                    roleId,
                    action.moduleAlias(),
                    action.actionCode(),
                    action.dataAuth() ? DataScopePolicy.ALL : DataScopePolicy.NONE,
                    TenantScopePolicy.CURRENT_TENANT
            );
        }
    }

    private boolean isActive(EntityContract entity) {
        return entity != null && !Boolean.TRUE.equals(entity.getDeleted())
                && (!(entity instanceof EnabledCapable enabled)
                || Boolean.TRUE.equals(enabled.getEnabled()));
    }
}
