package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.DataScopeAbility;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.MutationTenantScopeExecutor;
import net.ximatai.muyun.spring.boot.web.MutationTenantScopeResolver;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebOutputSupport;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.DataScopeCriteriaResult;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.role.RoleService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.user", title = "用户管理")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, order = 60)
@RequestMapping("/iam.user")
public class UserAccountWebController extends WebSupport<UserAccountService> implements
        CrudWeb<UserAccount, UserAccountService>,
        EnableWeb<UserAccount, UserAccountService>,
        MutationTenantScopeResolver<UserAccount>,
        StaticModuleUiContributor {
    private static final ActionExecutionPolicy USER_SELECTOR_POLICY = new ActionExecutionPolicy(
            "userSelector",
            PlatformActionLevel.LIST,
            ActionAccessMode.AUTH_REQUIRED,
            true,
            true,
            ActionDefaultGrantPolicy.NONE,
            null
    );

    private final UserSessionService userSessionService;
    private final RoleService roleService;
    private final EmployeeAccountService employeeAccountService;
    private final EmployeeService employeeService;
    private StaticRecordReadProjectionService staticRecordReadProjectionService;

    public UserAccountWebController(ObjectProvider<UserSessionService> userSessionService) {
        this(userSessionService, null, null, null);
    }

    public UserAccountWebController(ObjectProvider<UserSessionService> userSessionService,
                                    ObjectProvider<RoleService> roleService) {
        this(userSessionService, roleService, null, null);
    }

    @Autowired
    public UserAccountWebController(ObjectProvider<UserSessionService> userSessionService,
                                    ObjectProvider<RoleService> roleService,
                                    ObjectProvider<EmployeeAccountService> employeeAccountService,
                                    ObjectProvider<EmployeeService> employeeService) {
        this.userSessionService = userSessionService == null ? null : userSessionService.getIfAvailable();
        this.roleService = roleService == null ? null : roleService.getIfAvailable();
        this.employeeAccountService = employeeAccountService == null ? null : employeeAccountService.getIfAvailable();
        this.employeeService = employeeService == null ? null : employeeService.getIfAvailable();
    }

    @Autowired(required = false)
    void setStaticRecordReadProjectionService(StaticRecordReadProjectionService staticRecordReadProjectionService) {
        this.staticRecordReadProjectionService = staticRecordReadProjectionService;
    }

    @Override
    public StaticRecordReadProjectionService staticRecordReadProjectionService() {
        return staticRecordReadProjectionService;
    }

    @Override
    public ModuleUiDefinition moduleUiDefinition() {
        return ModuleUiDefinition.builder(UserAccountService.MODULE_ALIAS)
                .listView(list -> list
                        .title("用户列表")
                        .field("username", field -> field.label("账号").width("180px"))
                        .field("enabled", field -> field.label("状态").uiType("enabledStatus")
                                .width("90px").align("center"))
                        .field("passwordStatus", field -> field.label("密码状态").width("120px"))
                        .field("employeeNo", field -> field.label("职员工号").width("150px"))
                        .field("employeeTitle", field -> field.label("职员姓名").width("150px"))
                        .field("lastLoginAt", field -> field.label("最后登录时间").width("180px")))
                .formView(form -> form
                        .title("用户账号")
                        .field("username", field -> field.label("账号").required())
                        .field("enabled", field -> field.label("启用状态").uiType("enabledStatus"))
                        .field("passwordStatus", field -> field.label("密码状态").readOnly())
                        .field("lastLoginAt", field -> field.label("最后登录时间").readOnly()))
                .build();
    }

    @Override
    @GetMapping("/view/{id}")
    @ActionEndpoint(PlatformAction.VIEW)
    public UserAccount view(@PathVariable String id) {
        return webScope(() -> WebOutputSupport.record(service(),
                service().selectForView(id), FieldOutputContext.VIEW));
    }

    @PostMapping("/changePassword/{id}")
    @CustomActionEndpoint(value = "changePassword", title = "修改密码",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WebCountResponse changePassword(@PathVariable String id,
                                           @RequestBody ChangePasswordRequest request) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            int changed = service().changePassword(id, request.password());
            if (changed > 0 && userSessionService != null) {
                userSessionService.revokeUserSessions(id);
            }
            return new WebCountResponse(changed);
        }));
    }

    @PostMapping("/resetPassword/{id}")
    @CustomActionEndpoint(value = "resetPassword", title = "重置密码",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public ResetPasswordResponse resetPassword(@PathVariable String id) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            UserAccountService.PasswordResetResult result = service().resetPassword(id);
            if (result.count() > 0 && userSessionService != null) {
                userSessionService.revokeUserSessions(id);
            }
            return new ResetPasswordResponse(result.count(), result.temporaryPassword(), result.expiresAt());
        }));
    }

    @Override
    public Optional<String> tenantIdForCreate(UserAccount record) {
        return tenantIdForUser(record);
    }

    @Override
    public Optional<String> tenantIdForUpdate(String id, UserAccount record) {
        UserAccount existing = service().select(id);
        if (existing != null) {
            return tenantIdForUser(existing);
        }
        return tenantIdForCreate(record);
    }

    @Override
    public Optional<String> tenantIdForExistingRecord(String id) {
        return tenantIdForUser(service().select(id));
    }

    @PostMapping("/selector/query")
    @CustomActionEndpoint(value = "userSelector", title = "用户选择器", level = PlatformActionLevel.LIST,
            dataAuth = true)
    public WebPageResponse<UserSelectorItem> selector(@RequestBody(required = false) UserSelectorRequest request) {
        return webScope(() -> {
            UserSelectorRequest normalized = request == null ? UserSelectorRequest.EMPTY : request;
            Criteria criteria = selectorCriteria(normalized);
            WebPageRequest page = normalized.pageOrDefault();
            PageResult<UserAccount> result = selectorPageQuery(criteria,
                    PageRequest.of(page.pageNum(), page.pageSize()), Sort.asc("username"));
            return WebPageResponse.from(PageResult.of(
                    result.getRecords().stream().map(UserSelectorItem::from).toList(),
                    result.getTotal(),
                    PageRequest.of(result.getPageNum(), result.getPageSize())
            ));
        });
    }

    @GetMapping("/{id}/employee-binding")
    @CustomActionEndpoint(value = "employeeBinding", title = "绑定职员",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public UserEmployeeBindingView employeeBinding(@PathVariable String id) {
        return MutationTenantScopeExecutor.forExistingRecord(this, id, () -> webScope(() -> {
            if (employeeAccountService == null || employeeService == null) {
                return UserEmployeeBindingView.empty();
            }
            EmployeeAccount binding = employeeAccountService.accountOfUser(id);
            if (binding == null) {
                return UserEmployeeBindingView.empty();
            }
            Employee employee = employeeService.select(binding.getEmployeeId());
            return UserEmployeeBindingView.from(binding, employee);
        }));
    }

    public record ChangePasswordRequest(String password) {
    }

    public record ResetPasswordResponse(int count, String temporaryPassword, java.time.Instant expiresAt) {
    }

    public record UserSelectorRequest(
            String roleId,
            String keyword,
            Boolean enabledOnly,
            WebPageRequest page
    ) {
        static final UserSelectorRequest EMPTY = new UserSelectorRequest(null, null, Boolean.TRUE, null);

        WebPageRequest pageOrDefault() {
            return page == null ? WebPageRequest.DEFAULT : page;
        }
    }

    public record UserSelectorItem(
            String id,
            String username
    ) {
        static UserSelectorItem from(UserAccount user) {
            return new UserSelectorItem(
                    user.getId(),
                    user.getUsername()
            );
        }
    }

    public record UserEmployeeBindingView(
            String bindingId,
            String employeeId,
            String employeeNo,
            String employeeTitle,
            String organizationId,
            String departmentId
    ) {
        static UserEmployeeBindingView empty() {
            return new UserEmployeeBindingView(null, null, null, null, null, null);
        }

        static UserEmployeeBindingView from(EmployeeAccount binding, Employee employee) {
            return new UserEmployeeBindingView(
                    binding.getId(),
                    binding.getEmployeeId(),
                    employee == null ? null : employee.getEmployeeNo(),
                    employee == null ? null : employee.getTitle(),
                    employee == null ? null : employee.getOrganizationId(),
                    employee == null ? null : employee.getDepartmentId()
            );
        }
    }

    private Criteria selectorCriteria(UserSelectorRequest request) {
        Criteria criteria = Criteria.of();
        if (!Boolean.FALSE.equals(request.enabledOnly())) {
            criteria.eq("enabled", Boolean.TRUE);
        }
        if (request.roleId() != null && !request.roleId().isBlank()) {
            if (roleService == null) {
                throw new IllegalStateException("role service is not available");
            }
            java.util.List<String> userIds = roleService.userIds(request.roleId());
            if (userIds.isEmpty()) {
                criteria.in("id", java.util.List.of("__none__"));
            } else {
                criteria.in("id", userIds);
            }
        }
        if (request.keyword() != null && !request.keyword().isBlank()) {
            String keyword = request.keyword().trim();
            Criteria keywordCriteria = Criteria.of();
            keywordCriteria.orGroup(Criteria.of().like("username", keyword).getRoot());
            criteria.andGroup(keywordCriteria.getRoot());
        }
        return criteria;
    }

    private PageResult<UserAccount> selectorPageQuery(Criteria criteria, PageRequest pageRequest, Sort sort) {
        if (service() instanceof DataScopeAbility<?>) {
            DataScopeAbility<UserAccount> dataScopeAbility = DataScopeAbility.cast(service());
            DataScopeCriteriaResult scope = dataScopeAbility.readScopeByPolicy(selectorPolicy(), criteria);
            return dataScopeAbility.withDataScopeTenant(scope,
                    () -> service().pageQuery(scope.criteria(), pageRequest, sort));
        }
        return service().pageQuery(criteria, pageRequest, sort);
    }

    private ActionExecutionPolicy selectorPolicy() {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(webScopeName()))
                .map(ActionExecutionContext::actionPolicy)
                .orElse(USER_SELECTOR_POLICY);
    }

    private Optional<String> tenantIdForUser(UserAccount user) {
        if (user == null || user.getTenantId() == null || user.getTenantId().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(user.getTenantId().trim());
    }
}
