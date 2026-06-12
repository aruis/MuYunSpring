package net.ximatai.muyun.spring.boot.iam;

import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PlatformStaticModule(application = "iam", alias = "iam.employee", title = "职员管理")
@RequestMapping("/iam.employee")
public class EmployeeWebController extends WebSupport<EmployeeService> implements
        CrudWeb<Employee, EmployeeService>,
        EnableWeb<Employee, EmployeeService>,
        SortWeb<Employee, EmployeeService> {
    private final EmployeePositionService employeePositionService;
    private final EmployeeAccountService employeeAccountService;
    private final EmployeeDelegationService employeeDelegationService;

    @Autowired
    public EmployeeWebController(EmployeePositionService employeePositionService,
                                 EmployeeAccountService employeeAccountService,
                                 EmployeeDelegationService employeeDelegationService) {
        this.employeePositionService = employeePositionService;
        this.employeeAccountService = employeeAccountService;
        this.employeeDelegationService = employeeDelegationService;
    }

    @GetMapping("/{employeeId}/accounts")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeeAccount> accounts(@PathVariable String employeeId) {
        return webScope(() -> new WebListResponse<>(employeeAccountService.accounts(employeeId)));
    }

    @PostMapping("/{employeeId}/accounts")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeAccount bindAccount(@PathVariable String employeeId,
                                       @RequestBody EmployeeAccount binding) {
        return webScope(() -> employeeAccountService.select(employeeAccountService.bindAccount(employeeId, binding)));
    }

    @PostMapping("/{employeeId}/accounts/{bindingId}/delete")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse deleteAccount(@PathVariable String employeeId,
                                          @PathVariable String bindingId) {
        return webScope(() -> new WebCountResponse(employeeAccountService.deleteAccount(employeeId, bindingId)));
    }

    @PostMapping("/{employeeId}/accounts/{bindingId}/enable")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse enableAccount(@PathVariable String employeeId,
                                          @PathVariable String bindingId) {
        return webScope(() -> new WebCountResponse(employeeAccountService.enableAccount(employeeId, bindingId)));
    }

    @PostMapping("/{employeeId}/accounts/{bindingId}/disable")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse disableAccount(@PathVariable String employeeId,
                                           @PathVariable String bindingId) {
        return webScope(() -> new WebCountResponse(employeeAccountService.disableAccount(employeeId, bindingId)));
    }

    @PostMapping("/{employeeId}/accounts/{bindingId}/primary")
    @CustomActionEndpoint(value = "employeeAccounts", title = "职员账号",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse makePrimaryAccount(@PathVariable String employeeId,
                                               @PathVariable String bindingId) {
        return webScope(() -> new WebCountResponse(employeeAccountService.makePrimaryAccount(employeeId, bindingId)));
    }

    @GetMapping("/{employeeId}/positions")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeePosition> positions(@PathVariable String employeeId) {
        return webScope(() -> new WebListResponse<>(employeePositionService.positions(employeeId)));
    }

    @PostMapping("/{employeeId}/positions")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeePosition addPosition(@PathVariable String employeeId,
                                        @RequestBody EmployeePosition relation) {
        return webScope(() -> employeePositionService.select(employeePositionService.addPosition(employeeId, relation)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/update")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeePosition updatePosition(@PathVariable String employeeId,
                                           @PathVariable String relationId,
                                           @RequestBody EmployeePosition relation) {
        return webScope(() -> {
            employeePositionService.updatePosition(employeeId, relationId, relation);
            return employeePositionService.select(relationId);
        });
    }

    @PostMapping("/{employeeId}/positions/{relationId}/delete")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse deletePosition(@PathVariable String employeeId,
                                           @PathVariable String relationId) {
        return webScope(() -> new WebCountResponse(employeePositionService.deletePosition(employeeId, relationId)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/enable")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse enablePosition(@PathVariable String employeeId,
                                           @PathVariable String relationId) {
        return webScope(() -> new WebCountResponse(employeePositionService.enablePosition(employeeId, relationId)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/disable")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse disablePosition(@PathVariable String employeeId,
                                            @PathVariable String relationId) {
        return webScope(() -> new WebCountResponse(employeePositionService.disablePosition(employeeId, relationId)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/primary")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse makePrimaryPosition(@PathVariable String employeeId,
                                                @PathVariable String relationId) {
        return webScope(() -> new WebCountResponse(employeePositionService.makePrimaryPosition(employeeId, relationId)));
    }

    @PostMapping("/{employeeId}/positions/{relationId}/sort")
    @CustomActionEndpoint(value = "employeePositions", title = "职员任岗",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse sortPosition(@PathVariable String employeeId,
                                         @PathVariable String relationId,
                                         @RequestBody(required = false) SortWebRequest request) {
        return webScope(() -> {
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
        return webScope(() -> new WebListResponse<>(employeeDelegationService.delegationsByPrincipal(employeeId)));
    }

    @GetMapping("/{employeeId}/delegated-to-me")
    @CustomActionEndpoint(value = "employeeDelegatedToMe", title = "职员受托代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebListResponse<EmployeeDelegation> delegatedToMe(@PathVariable String employeeId) {
        return webScope(() -> new WebListResponse<>(employeeDelegationService.delegationsByDelegate(employeeId)));
    }

    @PostMapping("/{employeeId}/delegations")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeDelegation addDelegation(@PathVariable String employeeId,
                                            @RequestBody EmployeeDelegation delegation) {
        return webScope(() -> employeeDelegationService.select(
                employeeDelegationService.addDelegation(employeeId, delegation)));
    }

    @PostMapping("/{employeeId}/delegations/{delegationId}/update")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public EmployeeDelegation updateDelegation(@PathVariable String employeeId,
                                               @PathVariable String delegationId,
                                               @RequestBody EmployeeDelegation delegation) {
        return webScope(() -> {
            employeeDelegationService.updateDelegation(employeeId, delegationId, delegation);
            return employeeDelegationService.select(delegationId);
        });
    }

    @PostMapping("/{employeeId}/delegations/{delegationId}/delete")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse deleteDelegation(@PathVariable String employeeId,
                                             @PathVariable String delegationId) {
        return webScope(() -> new WebCountResponse(
                employeeDelegationService.deleteDelegation(employeeId, delegationId)));
    }

    @PostMapping("/{employeeId}/delegations/{delegationId}/enable")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse enableDelegation(@PathVariable String employeeId,
                                             @PathVariable String delegationId) {
        return webScope(() -> new WebCountResponse(
                employeeDelegationService.enableDelegation(employeeId, delegationId)));
    }

    @PostMapping("/{employeeId}/delegations/{delegationId}/disable")
    @CustomActionEndpoint(value = "employeeDelegations", title = "职员业务代办",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "employeeId")
    public WebCountResponse disableDelegation(@PathVariable String employeeId,
                                              @PathVariable String delegationId) {
        return webScope(() -> new WebCountResponse(
                employeeDelegationService.disableDelegation(employeeId, delegationId)));
    }
}
