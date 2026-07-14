package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
import net.ximatai.muyun.spring.boot.web.BusinessMutation;
import net.ximatai.muyun.spring.boot.web.MutationTenantScopeExecutor;
import net.ximatai.muyun.spring.boot.web.MutationTenantScopeResolver;
import net.ximatai.muyun.spring.boot.web.SortWeb;
import net.ximatai.muyun.spring.boot.web.SortWebRequest;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebSupport;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.iam.employee.Employee;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccount;
import net.ximatai.muyun.spring.iam.employee.EmployeeAccountService;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegation;
import net.ximatai.muyun.spring.iam.employee.EmployeeDelegationService;
import net.ximatai.muyun.spring.iam.employee.EmployeePosition;
import net.ximatai.muyun.spring.iam.employee.EmployeePositionService;
import net.ximatai.muyun.spring.iam.employee.EmployeeService;
import net.ximatai.muyun.spring.iam.organization.Organization;
import net.ximatai.muyun.spring.iam.organization.OrganizationService;
import net.ximatai.muyun.spring.iam.user.UserAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.employee", title = "职员管理", route = "/iam/employees")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, title = "职员管理", order = 50)
@RequestMapping("/iam.employee")
public class EmployeeWebController extends WebSupport<EmployeeService> implements
        CrudWeb<Employee, EmployeeService>,
        EnableWeb<Employee, EmployeeService>,
        SortWeb<Employee, EmployeeService>,
        MutationTenantScopeResolver<Employee>,
        StaticModuleUiContributor {
    private final EmployeePositionService employeePositionService;
    private final EmployeeAccountService employeeAccountService;
    private final EmployeeDelegationService employeeDelegationService;
    private OrganizationService organizationService;
    private StaticRecordReadProjectionService staticRecordReadProjectionService;

    @Autowired
    public EmployeeWebController(EmployeePositionService employeePositionService,
                                 EmployeeAccountService employeeAccountService,
                                 EmployeeDelegationService employeeDelegationService) {
        this.employeePositionService = employeePositionService;
        this.employeeAccountService = employeeAccountService;
        this.employeeDelegationService = employeeDelegationService;
    }

    @Autowired
    void setOrganizationService(OrganizationService organizationService) {
        this.organizationService = organizationService;
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
        return ModuleUiDefinition.builder(EmployeeService.MODULE_ALIAS)
                .listView(list -> list
                        .title("职员列表")
                        .field("employeeNo", field -> field.label("职员编号").width("150px"))
                        .field("organizationTitle", field -> field.label("所属机构").width("160px"))
                        .field("title", field -> field.label("职员姓名").width("150px"))
                        .field("username", field -> field.label("账号").width("150px"))
                        .field("mobile", field -> field.label("手机号").width("150px"))
                        .field("email", field -> field.label("邮箱"))
                        .field("enabled", field -> field.label("状态").uiType("enabledStatus")
                                .width("90px").align("center"))
                        .field("accountBound", field -> field.hidden()))
                .formView(form -> form
                        .title("职员档案")
                        .field("organizationId", field -> field.label("所属机构").required().readOnly())
                        .field("departmentId", field -> field.label("所属部门").required().uiType("recordPicker"))
                        .field("employeeNo", field -> field.label("职员编号").required())
                        .field("title", field -> field.label("职员姓名").required())
                        .field("gender", field -> field.label("性别"))
                        .field("mobile", field -> field.label("手机号"))
                        .field("email", field -> field.label("邮箱"))
                        .field("enabled", field -> field.label("启用状态").uiType("enabledStatus")))
                .build();
    }

    @Override
    public Optional<String> tenantIdForCreate(Employee record) {
        return tenantIdForEmployee(record);
    }

    @Override
    public Optional<String> tenantIdForUpdate(String id, Employee record) {
        Employee existing = service().select(id);
        if (existing != null) {
            return tenantIdForEmployee(existing);
        }
        return tenantIdForCreate(record);
    }

    @Override
    public Optional<String> tenantIdForExistingRecord(String id) {
        return tenantIdForEmployee(service().select(id));
    }

    @GetMapping("/{employeeId}/account")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeAccount account(@PathVariable String employeeId) {
        return employeeRecordScope(employeeId, () -> employeeAccountService.accountOfEmployee(employeeId));
    }

    @PostMapping("/{employeeId}/account")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeAccount bindAccount(@PathVariable String employeeId,
                                       @RequestBody EmployeeAccount binding) {
        return employeeRecordScope(employeeId,
                () -> employeeAccountService.select(employeeAccountService.bindAccount(employeeId, binding)));
    }

    @PostMapping("/{employeeId}/account/provision")
    @BusinessMutation
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public AccountProvisionResponse provisionAccount(@PathVariable String employeeId,
                                                     @RequestBody UserAccount account) {
        return employeeRecordScope(employeeId, () -> {
            EmployeeAccountService.AccountProvisionResult result =
                    employeeAccountService.provisionAccount(employeeId, account);
            return new AccountProvisionResponse(result.user(), result.binding());
        });
    }

    @PostMapping("/{employeeId}/account/delete")
    @BusinessMutation
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public void deleteAccount(@PathVariable String employeeId) {
        employeeRecordScope(employeeId, () -> {
            employeeAccountService.removeAccount(employeeId);
            return null;
        });
    }

    @GetMapping("/{employeeId}/positions")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeePosition> positions(@PathVariable String employeeId) {
        return employeeRecordScope(employeeId,
                () -> new WebListResponse<>(employeePositionService.positions(employeeId)));
    }

    @PostMapping("/{employeeId}/positions")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeePosition addPosition(@PathVariable String employeeId,
                                        @RequestBody EmployeePosition relation) {
        return employeeRecordScope(employeeId,
                () -> employeePositionService.select(employeePositionService.addPosition(employeeId, relation)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/update")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeePosition updatePosition(@PathVariable String employeeId,
                                           @PathVariable String relationId,
                                           @RequestBody EmployeePosition relation) {
        return employeeRecordScope(employeeId, () -> {
            employeePositionService.updatePosition(employeeId, relationId, relation);
            return employeePositionService.select(relationId);
        });
    }

    @PostMapping("/{employeeId}/positions/{relationId}/delete")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse deletePosition(@PathVariable String employeeId,
                                           @PathVariable String relationId) {
        return employeeRecordScope(employeeId,
                () -> new WebCountResponse(employeePositionService.deletePosition(employeeId, relationId)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/enable")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse enablePosition(@PathVariable String employeeId,
                                           @PathVariable String relationId) {
        return employeeRecordScope(employeeId,
                () -> new WebCountResponse(employeePositionService.enablePosition(employeeId, relationId)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/disable")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse disablePosition(@PathVariable String employeeId,
                                            @PathVariable String relationId) {
        return employeeRecordScope(employeeId,
                () -> new WebCountResponse(employeePositionService.disablePosition(employeeId, relationId)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/primary")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse makePrimaryPosition(@PathVariable String employeeId,
                                                @PathVariable String relationId) {
        return employeeRecordScope(employeeId,
                () -> new WebCountResponse(employeePositionService.makePrimaryPosition(employeeId, relationId)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/sort")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse sortPosition(@PathVariable String employeeId,
                                         @PathVariable String relationId,
                                         @RequestBody(required = false) SortWebRequest request) {
        return employeeRecordScope(employeeId, () -> {
            SortWebRequest normalized = request == null ? new SortWebRequest(null, null) : request;
            employeePositionService.moveEmployeePosition(employeeId, relationId,
                    normalized.previousId(), normalized.nextId());
            return new WebCountResponse(1);
        });
    }

    @GetMapping("/{employeeId}/delegations")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeeDelegation> delegations(@PathVariable String employeeId) {
        return employeeRecordScope(employeeId,
                () -> new WebListResponse<>(employeeDelegationService.delegationsByPrincipal(employeeId)));
    }

    @GetMapping("/{employeeId}/delegated-to-me")
    @CustomActionEndpoint(value = "employeeDelegatedToMe", title = "职员受托代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeeDelegation> delegatedToMe(@PathVariable String employeeId) {
        return employeeRecordScope(employeeId,
                () -> new WebListResponse<>(employeeDelegationService.delegationsByDelegate(employeeId)));
    }

    @PostMapping("/{employeeId}/delegations")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeDelegation addDelegation(@PathVariable String employeeId,
                                            @RequestBody EmployeeDelegation delegation) {
        return employeeRecordScope(employeeId, () -> employeeDelegationService.select(
                employeeDelegationService.addDelegation(employeeId, delegation)));
    }

    @PostMapping("/{employeeId}/delegations/{delegationId}/update")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeDelegation updateDelegation(@PathVariable String employeeId,
                                               @PathVariable String delegationId,
                                               @RequestBody EmployeeDelegation delegation) {
        return employeeRecordScope(employeeId, () -> {
            employeeDelegationService.updateDelegation(employeeId, delegationId, delegation);
            return employeeDelegationService.select(delegationId);
        });
    }

    @PostMapping("/{employeeId}/delegations/{delegationId}/delete")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse deleteDelegation(@PathVariable String employeeId,
                                             @PathVariable String delegationId) {
        return employeeRecordScope(employeeId, () -> new WebCountResponse(
                employeeDelegationService.deleteDelegation(employeeId, delegationId)));
    }

    @PostMapping("/{employeeId}/delegations/{delegationId}/enable")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse enableDelegation(@PathVariable String employeeId,
                                             @PathVariable String delegationId) {
        return employeeRecordScope(employeeId, () -> new WebCountResponse(
                employeeDelegationService.enableDelegation(employeeId, delegationId)));
    }

    @PostMapping("/{employeeId}/delegations/{delegationId}/disable")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse disableDelegation(@PathVariable String employeeId,
                                              @PathVariable String delegationId) {
        return employeeRecordScope(employeeId, () -> new WebCountResponse(
                employeeDelegationService.disableDelegation(employeeId, delegationId)));
    }

    private <R> R employeeRecordScope(String employeeId, Supplier<R> action) {
        return MutationTenantScopeExecutor.forExistingRecord(this, employeeId, () -> webScope(action));
    }

    public record AccountProvisionResponse(UserAccount user, EmployeeAccount binding) {
    }

    private Optional<String> tenantIdForEmployee(Employee employee) {
        if (employee == null) {
            return Optional.empty();
        }
        if (employee.getTenantId() != null && !employee.getTenantId().isBlank()) {
            return Optional.of(employee.getTenantId().trim());
        }
        String organizationId = employee.getOrganizationId();
        if (organizationId == null || organizationId.isBlank()) {
            return Optional.empty();
        }
        Organization organization = organizationService.requireEnabled(organizationId,
                "organization is not active: " + organizationId);
        return Optional.of(net.ximatai.muyun.spring.common.util.Preconditions.requireText(
                organization.getTenantId(), "organization.tenantId"));
    }
}
