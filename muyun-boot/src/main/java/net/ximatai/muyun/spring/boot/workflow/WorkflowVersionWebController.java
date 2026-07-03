package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.NestedCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebRecordResponse;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowDefinitionService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowPublishStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersion;
import net.ximatai.muyun.spring.platform.workflow.WorkflowVersionService;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = WorkflowVersionService.MODULE_ALIAS,
        title = "平台工作流版本")
@Path("/platform.module/{moduleAlias}/workflow-definitions/{definitionId}/versions")
public class WorkflowVersionWebController extends NestedCrudWebSupport<WorkflowVersion, WorkflowVersionService> {

    private final WorkflowDefinitionService definitionService;

    public WorkflowVersionWebController(WorkflowDefinitionService definitionService) {
        this.definitionService = Objects.requireNonNull(definitionService, "definitionService must not be null");
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        requireDefinition(request);
        criteria.eq("definitionId", definitionId(request));
    }

    @Override
    protected void bindScope(WorkflowVersion record, WebRequestScope request) {
        requireDefinition(request);
        record.setDefinitionId(definitionId(request));
    }

    @Override
    protected boolean inScope(WorkflowVersion record, WebRequestScope request) {
        requireDefinition(request);
        return definitionId(request).equals(record.getDefinitionId());
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "workflow version does not belong to definition: " + definitionId(request) + "." + id;
    }

    @Override
    @ActionEndpoint(PlatformAction.CREATE)
    public WebRecordResponse<WorkflowVersion> insert(@Context UriInfo uriInfo,
                                                     WorkflowVersion record) {
        normalizeDraft(record);
        return super.insert(uriInfo, record);
    }

    @Override
    @ActionEndpoint(PlatformAction.UPDATE)
    public WebRecordResponse<WorkflowVersion> update(@Context UriInfo uriInfo,
                                                     @PathParam("id") String id,
                                                     WorkflowVersion record) {
        requireDraft(requireScopedRecord(requestScope(uriInfo), id),
                "workflow version can only edit draft versions");
        normalizeDraft(record);
        return super.update(uriInfo, id, record);
    }

    @Override
    @ActionEndpoint(PlatformAction.DELETE)
    public WebCountResponse delete(@Context UriInfo uriInfo, @PathParam("id") String id) {
        requireDraft(requireScopedRecord(requestScope(uriInfo), id),
                "workflow version can only delete draft versions");
        return super.delete(uriInfo, id);
    }

    private WorkflowDefinition requireDefinition(WebRequestScope request) {
        String validModuleAlias = PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
        WorkflowDefinition definition = definitionService.select(definitionId(request));
        if (definition == null || !validModuleAlias.equals(definition.getModuleAlias())) {
            throw new IllegalArgumentException("workflow definition does not belong to module: "
                    + validModuleAlias + "." + definitionId(request));
        }
        return definition;
    }

    private String definitionId(WebRequestScope request) {
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
