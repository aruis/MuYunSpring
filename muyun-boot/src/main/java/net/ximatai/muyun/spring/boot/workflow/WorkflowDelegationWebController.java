package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.ScopedWeb;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryCondition;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.spring.boot.web.WebPageResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegation;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegationScopeType;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDelegationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping({"/platform.workflow_delegation", "/workflow/delegation"})
@PlatformStaticModule(application = "platform",
        alias = WorkflowDelegationService.MODULE_ALIAS,
        title = "Workflow Delegation")
public class WorkflowDelegationWebController implements ScopedWeb<WorkflowDelegationService> {
    private static final Set<String> QUERY_ALLOWED_FIELDS = Set.of(
            "title", "enabled", "principalCanProcess", "moduleScopeType", "orgScopeType");

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
        Criteria criteria = criteria(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageByPrincipal(currentUserId(), criteria, page));
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
        Criteria criteria = criteria(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageByDelegate(currentUserId(), criteria, page));
    }

    @PostMapping("/manage/query")
    @CustomActionEndpoint(value = "manageQuery", title = "Delegation Manage Query",
            level = PlatformActionLevel.LIST)
    public WebPageResponse<WorkflowDelegation> manageQuery(@RequestBody(required = false) WebQueryRequest request) {
        Criteria criteria = criteria(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageQuery(criteria, page, Sort.desc("updatedAt"),
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

    private Criteria criteria(WebQueryRequest request) {
        rejectSorts(request);
        Criteria criteria = Criteria.of();
        if (request == null || request.conditions().isEmpty()) {
            return criteria;
        }
        for (WebQueryCondition condition : request.conditions()) {
            appendCondition(criteria, condition);
        }
        return criteria;
    }

    private void rejectSorts(WebQueryRequest request) {
        if (request != null && !request.sorts().isEmpty()) {
            throw new PlatformException("workflow delegation web query does not support custom sorts");
        }
    }

    private void appendCondition(Criteria criteria, WebQueryCondition condition) {
        String field = requireAllowedField(condition.fieldName());
        requireEqOperator(condition.operator(), field);
        if (condition.values().isEmpty() || condition.values().getFirst() == null) {
            return;
        }
        Object value = conditionValue(field, condition.values().getFirst());
        if (value != null) {
            criteria.eq(field, value);
        }
    }

    private String requireAllowedField(String fieldName) {
        String field = fieldName == null ? "" : fieldName.trim();
        if (!QUERY_ALLOWED_FIELDS.contains(field)) {
            throw new PlatformException("workflow delegation web query field is not allowed: " + field);
        }
        return field;
    }

    private void requireEqOperator(String operator, String field) {
        if (operator == null || operator.isBlank() || "EQ".equalsIgnoreCase(operator) || "=".equals(operator)) {
            return;
        }
        throw new PlatformException("workflow delegation web query only supports EQ operator for field: " + field);
    }

    private Object conditionValue(String field, Object value) {
        return switch (field) {
            case "enabled", "principalCanProcess" -> booleanValue(value, field);
            case "moduleScopeType", "orgScopeType" -> scopeType(value, field);
            default -> textValue(value);
        };
    }

    private String textValue(Object value) {
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private Boolean booleanValue(Object value, String field) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.valueOf(text);
        }
        throw new PlatformException("workflow delegation web query requires boolean value for field: " + field);
    }

    private WorkflowDelegationScopeType scopeType(Object value, String field) {
        String text = String.valueOf(value).trim();
        for (WorkflowDelegationScopeType type : WorkflowDelegationScopeType.values()) {
            if (type.name().equalsIgnoreCase(text) || type.getCode().equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new PlatformException("workflow delegation web query requires valid scope type for field: " + field);
    }

    private String currentUserId() {
        return CurrentUserContext.currentUser()
                .map(user -> user.userId())
                .filter(value -> value != null && !value.isBlank())
                .orElseThrow(() -> new PlatformException("current user is required for workflow delegation"));
    }
}
