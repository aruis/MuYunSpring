package net.ximatai.muyun.spring.boot.workflow;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.CustomActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import net.ximatai.muyun.spring.platform.module.PlatformModuleService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinitionService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinitionStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowPublishFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersion;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;

@RestController
@PlatformStaticModule(application = "platform", alias = WorkflowDefinitionService.MODULE_ALIAS,
        title = "平台工作流定义")
@RequestMapping("/platform.module/{moduleAlias}/workflow-definitions")
public class WorkflowDefinitionWebController
        extends NestedSortableCrudWebSupport<WorkflowDefinition, WorkflowDefinitionService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "applicationAlias", "moduleAlias", "alias", "approvalEnabled", "actionCode",
            "definitionStatus", "currentVersionNo", "title", "enabled", "sortOrder", "createdAt", "updatedAt");

    private final PlatformModuleService moduleService;
    private final WorkflowPublishFacade publishFacade;

    public WorkflowDefinitionWebController(PlatformModuleService moduleService,
                                           WorkflowPublishFacade publishFacade) {
        this.moduleService = Objects.requireNonNull(moduleService, "moduleService must not be null");
        this.publishFacade = Objects.requireNonNull(publishFacade, "publishFacade must not be null");
    }

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return WorkflowConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return WorkflowConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("sortOrder"));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        criteria.eq("moduleAlias", moduleAlias(request));
    }

    @Override
    protected void bindScope(WorkflowDefinition record, HttpServletRequest request) {
        PlatformModule module = requireModule(request);
        record.setApplicationAlias(module.getApplicationAlias());
        record.setModuleAlias(module.getAlias());
    }

    @Override
    protected boolean inScope(WorkflowDefinition record, HttpServletRequest request) {
        return moduleAlias(request).equals(record.getModuleAlias());
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "workflow definition does not belong to module: " + moduleAlias(request) + "." + id;
    }

    @Override
    @PostMapping("/insert")
    @ActionEndpoint(PlatformAction.CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowDefinition insert(HttpServletRequest servletRequest, @RequestBody WorkflowDefinition record) {
        normalizeDraft(record);
        return super.insert(servletRequest, record);
    }

    @Override
    @PostMapping("/update/{id}")
    @ActionEndpoint(PlatformAction.UPDATE)
    public WorkflowDefinition update(HttpServletRequest servletRequest,
                                     @PathVariable String id,
                                     @RequestBody WorkflowDefinition record) {
        requireDraft(requireScopedRecord(servletRequest, id), "workflow definition can only edit draft definitions");
        normalizeDraft(record);
        return super.update(servletRequest, id, record);
    }

    @Override
    @PostMapping("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    public WebCountResponse delete(HttpServletRequest servletRequest, @PathVariable String id) {
        requireDraft(requireScopedRecord(servletRequest, id), "workflow definition can only delete draft definitions");
        return super.delete(servletRequest, id);
    }

    @PostMapping("/{definitionId}/versions/{versionId}/publish")
    @CustomActionEndpoint(value = "publishWorkflowDefinition", title = "发布工作流定义",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "definitionId")
    public WorkflowVersion publish(HttpServletRequest request,
                                   @PathVariable String definitionId,
                                   @PathVariable String versionId) {
        return webScope(() -> {
            requireScopedRecord(request, definitionId);
            return publishFacade.publish(definitionId, versionId, currentOperatorIdOrNull());
        });
    }

    @PostMapping("/{definitionId}/disable")
    @CustomActionEndpoint(value = "disableWorkflowDefinition", title = "停用工作流定义",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "definitionId")
    public WorkflowDefinition disableDefinition(HttpServletRequest request, @PathVariable String definitionId) {
        return webScope(() -> {
            requireScopedRecord(request, definitionId);
            return publishFacade.disable(definitionId);
        });
    }

    @PostMapping("/{definitionId}/archive")
    @CustomActionEndpoint(value = "archiveWorkflowDefinition", title = "归档工作流定义",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "definitionId")
    public WorkflowDefinition archive(HttpServletRequest request, @PathVariable String definitionId) {
        return webScope(() -> {
            requireScopedRecord(request, definitionId);
            return publishFacade.archive(definitionId);
        });
    }

    private String moduleAlias(HttpServletRequest request) {
        return PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
    }

    private PlatformModule requireModule(HttpServletRequest request) {
        String validModuleAlias = moduleAlias(request);
        PlatformModule module = moduleService.resolveVisibleModule(validModuleAlias);
        if (module == null) {
            throw new IllegalArgumentException("platform module not found: " + validModuleAlias);
        }
        return module;
    }

    private void normalizeDraft(WorkflowDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("workflow definition must not be null");
        }
        definition.setDefinitionStatus(WorkflowDefinitionStatus.DRAFT);
        definition.setCurrentVersionNo(null);
    }

    private void requireDraft(WorkflowDefinition definition, String message) {
        if (definition.getDefinitionStatus() != WorkflowDefinitionStatus.DRAFT) {
            throw new IllegalArgumentException(message + ": " + definition.getId());
        }
    }

    private String currentOperatorIdOrNull() {
        return CurrentUserContext.currentUser()
                .map(user -> user.userId())
                .filter(userId -> !userId.isBlank())
                .orElse(null);
    }
}
