package net.ximatai.muyun.spring.boot.workflow;

import jakarta.servlet.http.HttpServletRequest;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebQueryRequest;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinitionService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowPublishStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersion;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersionService;
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
@PlatformStaticModule(application = "platform", alias = WorkflowVersionService.MODULE_ALIAS,
        title = "平台工作流版本")
@RequestMapping("/platform.module/{moduleAlias}/workflow-definitions/{definitionId}/versions")
public class WorkflowVersionWebController extends NestedCrudWebSupport<WorkflowVersion, WorkflowVersionService> {
    private static final Set<String> QUERY_FIELDS = Set.of(
            "id", "definitionId", "versionNo", "publishStatus", "publishedBy", "publishedAt",
            "createdAt", "updatedAt");

    private final WorkflowDefinitionService definitionService;

    public WorkflowVersionWebController(WorkflowDefinitionService definitionService) {
        this.definitionService = Objects.requireNonNull(definitionService, "definitionService must not be null");
    }

    @Override
    protected Criteria queryCriteria(WebQueryRequest request) {
        return WorkflowConfigWebQuerySupport.criteria(request, QUERY_FIELDS, webScopeName());
    }

    @Override
    protected Sort[] querySorts(WebQueryRequest request) {
        return WorkflowConfigWebQuerySupport.sorts(request, QUERY_FIELDS, Sort.asc("versionNo"));
    }

    @Override
    protected void appendScope(Criteria criteria, HttpServletRequest request) {
        requireDefinition(request);
        criteria.eq("definitionId", definitionId(request));
    }

    @Override
    protected void bindScope(WorkflowVersion record, HttpServletRequest request) {
        requireDefinition(request);
        record.setDefinitionId(definitionId(request));
    }

    @Override
    protected boolean inScope(WorkflowVersion record, HttpServletRequest request) {
        requireDefinition(request);
        return definitionId(request).equals(record.getDefinitionId());
    }

    @Override
    protected String scopedRecordNotFoundMessage(HttpServletRequest request, String id) {
        return "workflow version does not belong to definition: " + definitionId(request) + "." + id;
    }

    @Override
    @PostMapping("/insert")
    @ActionEndpoint(PlatformAction.CREATE)
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowVersion insert(HttpServletRequest servletRequest, @RequestBody WorkflowVersion record) {
        normalizeDraft(record);
        return super.insert(servletRequest, record);
    }

    @Override
    @PostMapping("/update/{id}")
    @ActionEndpoint(PlatformAction.UPDATE)
    public WorkflowVersion update(HttpServletRequest servletRequest,
                                  @PathVariable String id,
                                  @RequestBody WorkflowVersion record) {
        requireDraft(requireScopedRecord(servletRequest, id), "workflow version can only edit draft versions");
        normalizeDraft(record);
        return super.update(servletRequest, id, record);
    }

    @Override
    @PostMapping("/delete/{id}")
    @ActionEndpoint(PlatformAction.DELETE)
    public WebCountResponse delete(HttpServletRequest servletRequest, @PathVariable String id) {
        requireDraft(requireScopedRecord(servletRequest, id), "workflow version can only delete draft versions");
        return super.delete(servletRequest, id);
    }

    private WorkflowDefinition requireDefinition(HttpServletRequest request) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
        WorkflowDefinition definition = definitionService.select(definitionId(request));
        if (definition == null || !validModuleAlias.equals(definition.getModuleAlias())) {
            throw new IllegalArgumentException("workflow definition does not belong to module: "
                    + validModuleAlias + "." + definitionId(request));
        }
        return definition;
    }

    private String definitionId(HttpServletRequest request) {
        return pathVariable(request, "definitionId");
    }

    private void normalizeDraft(WorkflowVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("workflow version must not be null");
        }
        version.setPublishStatus(WorkflowPublishStatus.DRAFT);
        version.setPublishedBy(null);
        version.setPublishedAt(null);
    }

    private void requireDraft(WorkflowVersion version, String message) {
        if (version.getPublishStatus() != WorkflowPublishStatus.DRAFT) {
            throw new IllegalArgumentException(message + ": " + version.getId());
        }
    }
}
