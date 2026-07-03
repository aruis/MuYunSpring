package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformMenu;
import net.ximatai.muyun.spring.boot.platform.PlatformMenuGroups;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.platform.ModuleUiDefinition;
import net.ximatai.muyun.spring.boot.platform.StaticRecordReadProjectionService;
import net.ximatai.muyun.spring.boot.platform.StaticModuleUiContributor;
import net.ximatai.muyun.spring.boot.web.CrudWeb;
import net.ximatai.muyun.spring.boot.web.EnableWeb;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;

import java.util.List;

@ApplicationScoped
@PlatformStaticModule(application = "iam", alias = "iam.employee", title = "职员管理", route = "/iam/employees")
@PlatformMenu(parent = PlatformMenuGroups.IDENTITY, title = "职员管理", order = 50)
@Path("/iam.employee")
public class EmployeeWebController extends WebSupport<EmployeeService> implements
        CrudWeb<Employee, EmployeeService>,
        EnableWeb<Employee, EmployeeService>,
        SortWeb<Employee, EmployeeService>,
        StaticModuleUiContributor {
    private final EmployeePositionService employeePositionService;
    private final EmployeeAccountService employeeAccountService;
    private final EmployeeDelegationService employeeDelegationService;
    private StaticRecordReadProjectionService staticRecordReadProjectionService;

    @Inject
    public EmployeeWebController(EmployeePositionService employeePositionService,
                                 EmployeeAccountService employeeAccountService,
                                 EmployeeDelegationService employeeDelegationService) {
        this.employeePositionService = employeePositionService;
        this.employeeAccountService = employeeAccountService;
        this.employeeDelegationService = employeeDelegationService;
    }

    @Inject
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
                        .field("title", field -> field.label("职员姓名").width("150px"))
                        .field("mobile", field -> field.label("手机号").width("150px"))
                        .field("email", field -> field.label("邮箱"))
                        .field("enabled", field -> field.label("状态").uiType("enabledStatus")
                                .width("90px").align("center")))
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

    @GET
    @Path("/{employeeId}/accounts")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeeAccount> accounts(@PathParam("employeeId") String employeeId) {
        return webScope(() -> new WebListResponse<>(employeeAccountService.accounts(employeeId)));
    }

    @POST
    @Path("/{employeeId}/accounts")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeAccount bindAccount(@PathParam("employeeId") String employeeId,
                                       EmployeeAccount binding) {
        return webScope(() -> employeeAccountService.select(employeeAccountService.bindAccount(employeeId, binding)));
    }

    @POST
    @Path("/{employeeId}/accounts/{bindingId}/delete")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse deleteAccount(@PathParam("employeeId") String employeeId,
                                          @PathParam("bindingId") String bindingId) {
        return webScope(() -> new WebCountResponse(employeeAccountService.deleteAccount(employeeId, bindingId)));
    }

    @POST
    @Path("/{employeeId}/accounts/{bindingId}/enable")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse enableAccount(@PathParam("employeeId") String employeeId,
                                          @PathParam("bindingId") String bindingId) {
        return webScope(() -> new WebCountResponse(employeeAccountService.enableAccount(employeeId, bindingId)));
    }

    @POST
    @Path("/{employeeId}/accounts/{bindingId}/disable")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse disableAccount(@PathParam("employeeId") String employeeId,
                                           @PathParam("bindingId") String bindingId) {
        return webScope(() -> new WebCountResponse(employeeAccountService.disableAccount(employeeId, bindingId)));
    }

    @POST
    @Path("/{employeeId}/accounts/{bindingId}/primary")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse makePrimaryAccount(@PathParam("employeeId") String employeeId,
                                               @PathParam("bindingId") String bindingId) {
        return webScope(() -> new WebCountResponse(employeeAccountService.makePrimaryAccount(employeeId, bindingId)));
    }

    @GET
    @Path("/{employeeId}/positions")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeePosition> positions(@PathParam("employeeId") String employeeId) {
        return webScope(() -> new WebListResponse<>(employeePositionService.positions(employeeId)));
    }

    @POST
    @Path("/{employeeId}/positions")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeePosition addPosition(@PathParam("employeeId") String employeeId,
                                        EmployeePosition relation) {
        return webScope(() -> employeePositionService.select(employeePositionService.addPosition(employeeId, relation)));
    }

    @POST
    @Path("/{employeeId}/positions/{relationId}/update")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeePosition updatePosition(@PathParam("employeeId") String employeeId,
                                           @PathParam("relationId") String relationId,
                                           EmployeePosition relation) {
        return webScope(() -> {
            employeePositionService.updatePosition(employeeId, relationId, relation);
            return employeePositionService.select(relationId);
        });
    }

    @POST
    @Path("/{employeeId}/positions/{relationId}/delete")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse deletePosition(@PathParam("employeeId") String employeeId,
                                           @PathParam("relationId") String relationId) {
        return webScope(() -> new WebCountResponse(employeePositionService.deletePosition(employeeId, relationId)));
    }

    @POST
    @Path("/{employeeId}/positions/{relationId}/enable")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse enablePosition(@PathParam("employeeId") String employeeId,
                                           @PathParam("relationId") String relationId) {
        return webScope(() -> new WebCountResponse(employeePositionService.enablePosition(employeeId, relationId)));
    }

    @POST
    @Path("/{employeeId}/positions/{relationId}/disable")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse disablePosition(@PathParam("employeeId") String employeeId,
                                            @PathParam("relationId") String relationId) {
        return webScope(() -> new WebCountResponse(employeePositionService.disablePosition(employeeId, relationId)));
    }

    @POST
    @Path("/{employeeId}/positions/{relationId}/primary")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse makePrimaryPosition(@PathParam("employeeId") String employeeId,
                                                @PathParam("relationId") String relationId) {
        return webScope(() -> new WebCountResponse(employeePositionService.makePrimaryPosition(employeeId, relationId)));
    }

    @POST
    @Path("/{employeeId}/positions/{relationId}/sort")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse sortPosition(@PathParam("employeeId") String employeeId,
                                         @PathParam("relationId") String relationId,
                                         SortWebRequest request) {
        return webScope(() -> {
            SortWebRequest normalized = request == null ? new SortWebRequest(null, null) : request;
            employeePositionService.moveEmployeePosition(employeeId, relationId,
                    normalized.previousId(), normalized.nextId());
            return new WebCountResponse(1);
        });
    }

    @GET
    @Path("/{employeeId}/delegations")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeeDelegation> delegations(@PathParam("employeeId") String employeeId) {
        return webScope(() -> new WebListResponse<>(employeeDelegationService.delegationsByPrincipal(employeeId)));
    }

    @GET
    @Path("/{employeeId}/delegated-to-me")
    @CustomActionEndpoint(value = "employeeDelegatedToMe", title = "职员受托代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeeDelegation> delegatedToMe(@PathParam("employeeId") String employeeId) {
        return webScope(() -> new WebListResponse<>(employeeDelegationService.delegationsByDelegate(employeeId)));
    }

    @POST
    @Path("/{employeeId}/delegations")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeDelegation addDelegation(@PathParam("employeeId") String employeeId,
                                            EmployeeDelegation delegation) {
        return webScope(() -> employeeDelegationService.select(
                employeeDelegationService.addDelegation(employeeId, delegation)));
    }

    @POST
    @Path("/{employeeId}/delegations/{delegationId}/update")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeDelegation updateDelegation(@PathParam("employeeId") String employeeId,
                                               @PathParam("delegationId") String delegationId,
                                               EmployeeDelegation delegation) {
        return webScope(() -> {
            employeeDelegationService.updateDelegation(employeeId, delegationId, delegation);
            return employeeDelegationService.select(delegationId);
        });
    }

    @POST
    @Path("/{employeeId}/delegations/{delegationId}/delete")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse deleteDelegation(@PathParam("employeeId") String employeeId,
                                             @PathParam("delegationId") String delegationId) {
        return webScope(() -> new WebCountResponse(
                employeeDelegationService.deleteDelegation(employeeId, delegationId)));
    }

    @POST
    @Path("/{employeeId}/delegations/{delegationId}/enable")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse enableDelegation(@PathParam("employeeId") String employeeId,
                                             @PathParam("delegationId") String delegationId) {
        return webScope(() -> new WebCountResponse(
                employeeDelegationService.enableDelegation(employeeId, delegationId)));
    }

    @POST
    @Path("/{employeeId}/delegations/{delegationId}/disable")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse disableDelegation(@PathParam("employeeId") String employeeId,
                                              @PathParam("delegationId") String delegationId) {
        return webScope(() -> new WebCountResponse(
                employeeDelegationService.disableDelegation(employeeId, delegationId)));
    }
}
