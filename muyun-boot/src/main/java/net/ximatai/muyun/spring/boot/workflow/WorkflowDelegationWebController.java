package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ScopedWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegation;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/platform.workflow_delegation", "/workflow/delegation"})
@PlatformStaticModule(application = "platform",
        alias = WorkflowDelegationService.MODULE_ALIAS,
        title = "Workflow Delegation")
public class WorkflowDelegationWebController implements ScopedWeb<WorkflowDelegationService> {
    private final WorkflowDelegationService service;

    public WorkflowDelegationWebController(WorkflowDelegationService service) {
        this.service = service;
    }

    @Override
    public WorkflowDelegationService service() {
        return service;
    }

    @PostMapping("/query")
    @CustomActionEndpoint(value = "query", title = "Delegation Query", level = PlatformActionLevel.LIST)
    public WebPageResponse<WorkflowDelegation> query(@RequestBody(required = false) WebQueryRequest request) {
        rejectConditions(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageByPrincipal(currentUserId(), page));
    }

    @PostMapping("/insert")
    @CustomActionEndpoint(value = "create", title = "Delegation Create", level = PlatformActionLevel.LIST)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDelegation insert(@RequestBody WorkflowDelegation delegation) {
        return service.insertForPrincipal(delegation, currentUserId());
    }

    @PostMapping("/update/{id}")
    @CustomActionEndpoint(value = "update", title = "Delegation Update", level = PlatformActionLevel.RECORD,
            dataAuth = true)
    public WorkflowDelegation update(@PathVariable String id, @RequestBody WorkflowDelegation delegation) {
        return service.updateForPrincipal(id, delegation, currentUserId());
    }

    @PostMapping("/delete/{id}")
    @CustomActionEndpoint(value = "delete", title = "Delegation Delete", level = PlatformActionLevel.RECORD,
            dataAuth = true)
    public WebCountResponse delete(@PathVariable String id) {
        return new WebCountResponse(service.deleteForPrincipal(id, currentUserId()));
    }

    @PostMapping("/enable/{id}")
    @CustomActionEndpoint(value = "enable", title = "Delegation Enable", level = PlatformActionLevel.RECORD,
            dataAuth = true)
    public WorkflowDelegation enable(@PathVariable String id) {
        return service.enableForPrincipal(id, currentUserId());
    }

    @PostMapping("/disable/{id}")
    @CustomActionEndpoint(value = "disable", title = "Delegation Disable", level = PlatformActionLevel.RECORD,
            dataAuth = true)
    public WorkflowDelegation disable(@PathVariable String id) {
        return service.disableForPrincipal(id, currentUserId());
    }

    @PostMapping("/delegatedToMe/query")
    @CustomActionEndpoint(value = "delegatedToMeQuery", title = "Delegated To Me Query",
            level = PlatformActionLevel.LIST)
    public WebPageResponse<WorkflowDelegation> delegatedToMe(@RequestBody(required = false) WebQueryRequest request) {
        rejectConditions(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageByDelegate(currentUserId(), page));
    }

    @PostMapping("/manage/query")
    @CustomActionEndpoint(value = "manageQuery", title = "Delegation Manage Query",
            level = PlatformActionLevel.LIST)
    public WebPageResponse<WorkflowDelegation> manageQuery(@RequestBody(required = false) WebQueryRequest request) {
        rejectConditions(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageQuery(Criteria.of(), page, Sort.desc("updatedAt"),
                Sort.desc("createdAt")));
    }

    @PostMapping("/manage/insert")
    @CustomActionEndpoint(value = "manageCreate", title = "Delegation Manage Create",
            level = PlatformActionLevel.LIST)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDelegation manageInsert(@RequestBody WorkflowDelegation delegation) {
        String id = service.insert(delegation);
        return service.select(id);
    }

    @PostMapping("/manage/update/{id}")
    @CustomActionEndpoint(value = "manageUpdate", title = "Delegation Manage Update",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WorkflowDelegation manageUpdate(@PathVariable String id, @RequestBody WorkflowDelegation delegation) {
        delegation.setId(id);
        service.update(delegation);
        return service.select(id);
    }

    @PostMapping("/manage/delete/{id}")
    @CustomActionEndpoint(value = "manageDelete", title = "Delegation Manage Delete",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WebCountResponse manageDelete(@PathVariable String id) {
        return new WebCountResponse(service.delete(id));
    }

    @PostMapping("/manage/enable/{id}")
    @CustomActionEndpoint(value = "manageEnable", title = "Delegation Manage Enable",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WorkflowDelegation manageEnable(@PathVariable String id) {
        return service.enable(id);
    }

    @PostMapping("/manage/disable/{id}")
    @CustomActionEndpoint(value = "manageDisable", title = "Delegation Manage Disable",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WorkflowDelegation manageDisable(@PathVariable String id) {
        return service.disable(id);
    }

    private PageRequest page(WebQueryRequest request) {
        WebPageRequest webPage = request == null ? WebPageRequest.DEFAULT : request.pageOrDefault();
        return PageRequest.of(webPage.pageNum(), webPage.pageSize());
    }

    private void rejectConditions(WebQueryRequest request) {
        if (request != null && (!request.conditions().isEmpty() || !request.sorts().isEmpty())) {
            throw new PlatformException("workflow delegation web query does not support custom conditions or sorts");
        }
    }

    private String currentUserId() {
        return CurrentUserContext.currentUser()
                .map(user -> user.userId())
                .filter(value -> value != null && !value.isBlank())
                .orElseThrow(() -> new PlatformException("current user is required for workflow delegation"));
    }
}
