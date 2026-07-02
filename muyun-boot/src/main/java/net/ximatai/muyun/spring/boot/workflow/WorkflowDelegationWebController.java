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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Set;

@ApplicationScoped
@Path("/platform.workflow_delegation")
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

    @POST
    @Path("/query")
    @CustomActionEndpoint(value = "query", title = "Delegation Query", level = PlatformActionLevel.LIST)
    public WebPageResponse<WorkflowDelegation> query(WebQueryRequest request) {
        Criteria criteria = criteria(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageByPrincipal(currentUserId(), criteria, page));
    }

    @POST
    @Path("/insert")
    @CustomActionEndpoint(value = "create", title = "Delegation Create", level = PlatformActionLevel.LIST)
    public WorkflowDelegation insert(WorkflowDelegation delegation) {
        return service.insertForPrincipal(delegation, currentUserId());
    }

    @POST
    @Path("/update/{id}")
    @CustomActionEndpoint(value = "update", title = "Delegation Update", level = PlatformActionLevel.RECORD,
            dataAuth = true)
    public WorkflowDelegation update(@PathParam("id") String id, WorkflowDelegation delegation) {
        return service.updateForPrincipal(id, delegation, currentUserId());
    }

    @POST
    @Path("/delete/{id}")
    @CustomActionEndpoint(value = "delete", title = "Delegation Delete", level = PlatformActionLevel.RECORD,
            dataAuth = true)
    public WebCountResponse delete(@PathParam("id") String id) {
        return new WebCountResponse(service.deleteForPrincipal(id, currentUserId()));
    }

    @POST
    @Path("/enable/{id}")
    @CustomActionEndpoint(value = "enable", title = "Delegation Enable", level = PlatformActionLevel.RECORD,
            dataAuth = true)
    public WorkflowDelegation enable(@PathParam("id") String id) {
        return service.enableForPrincipal(id, currentUserId());
    }

    @POST
    @Path("/disable/{id}")
    @CustomActionEndpoint(value = "disable", title = "Delegation Disable", level = PlatformActionLevel.RECORD,
            dataAuth = true)
    public WorkflowDelegation disable(@PathParam("id") String id) {
        return service.disableForPrincipal(id, currentUserId());
    }

    @POST
    @Path("/delegatedToMe/query")
    @CustomActionEndpoint(value = "delegatedToMeQuery", title = "Delegated To Me Query",
            level = PlatformActionLevel.LIST)
    public WebPageResponse<WorkflowDelegation> delegatedToMe(WebQueryRequest request) {
        Criteria criteria = criteria(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageByDelegate(currentUserId(), criteria, page));
    }

    @POST
    @Path("/manage/query")
    @CustomActionEndpoint(value = "manageQuery", title = "Delegation Manage Query",
            level = PlatformActionLevel.LIST)
    public WebPageResponse<WorkflowDelegation> manageQuery(WebQueryRequest request) {
        Criteria criteria = criteria(request);
        PageRequest page = page(request);
        return WebPageResponse.from(service.pageQuery(criteria, page, Sort.desc("updatedAt"),
                Sort.desc("createdAt")));
    }

    @POST
    @Path("/manage/insert")
    @CustomActionEndpoint(value = "manageCreate", title = "Delegation Manage Create",
            level = PlatformActionLevel.LIST)
    public WorkflowDelegation manageInsert(WorkflowDelegation delegation) {
        String id = service.insert(delegation);
        return service.select(id);
    }

    @POST
    @Path("/manage/update/{id}")
    @CustomActionEndpoint(value = "manageUpdate", title = "Delegation Manage Update",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WorkflowDelegation manageUpdate(@PathParam("id") String id, WorkflowDelegation delegation) {
        delegation.setId(id);
        service.update(delegation);
        return service.select(id);
    }

    @POST
    @Path("/manage/delete/{id}")
    @CustomActionEndpoint(value = "manageDelete", title = "Delegation Manage Delete",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WebCountResponse manageDelete(@PathParam("id") String id) {
        return new WebCountResponse(service.delete(id));
    }

    @POST
    @Path("/manage/enable/{id}")
    @CustomActionEndpoint(value = "manageEnable", title = "Delegation Manage Enable",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WorkflowDelegation manageEnable(@PathParam("id") String id) {
        return service.enable(id);
    }

    @POST
    @Path("/manage/disable/{id}")
    @CustomActionEndpoint(value = "manageDisable", title = "Delegation Manage Disable",
            level = PlatformActionLevel.RECORD, dataAuth = true)
    public WorkflowDelegation manageDisable(@PathParam("id") String id) {
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
