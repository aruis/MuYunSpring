package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.boot.web.WebRequestScope;
import net.ximatai.muyun.spring.boot.platform.PlatformStaticModule;
import net.ximatai.muyun.spring.boot.web.NestedSortableCrudWebSupport;
import net.ximatai.muyun.spring.boot.web.WebCountResponse;
import net.ximatai.muyun.spring.boot.web.WebRecordResponse;
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
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Objects;

@ApplicationScoped
@PlatformStaticModule(application = "platform", alias = WorkflowDefinitionService.MODULE_ALIAS,
        title = "平台工作流定义")
@Path("/platform.module/{moduleAlias}/workflow-definitions")
public class WorkflowDefinitionWebController
        extends NestedSortableCrudWebSupport<WorkflowDefinition, WorkflowDefinitionService> {

    private final PlatformModuleService moduleService;
    private final WorkflowPublishFacade publishFacade;

    public WorkflowDefinitionWebController(PlatformModuleService moduleService,
                                           WorkflowPublishFacade publishFacade) {
        this.moduleService = Objects.requireNonNull(moduleService, "moduleService must not be null");
        this.publishFacade = Objects.requireNonNull(publishFacade, "publishFacade must not be null");
    }

    @Override
    protected void appendScope(Criteria criteria, WebRequestScope request) {
        criteria.eq("moduleAlias", moduleAlias(request));
    }

    @Override
    protected void bindScope(WorkflowDefinition record, WebRequestScope request) {
        PlatformModule module = requireModule(request);
        record.setApplicationAlias(module.getApplicationAlias());
        record.setModuleAlias(module.getAlias());
    }

    @Override
    protected boolean inScope(WorkflowDefinition record, WebRequestScope request) {
        return moduleAlias(request).equals(record.getModuleAlias());
    }

    @Override
    protected String scopedRecordNotFoundMessage(WebRequestScope request, String id) {
        return "workflow definition does not belong to module: " + moduleAlias(request) + "." + id;
    }

    @Override
    @ActionEndpoint(PlatformAction.CREATE)
    public WebRecordResponse<WorkflowDefinition> insert(@Context UriInfo uriInfo,
                                                        WorkflowDefinition record) {
        normalizeDraft(record);
        return super.insert(uriInfo, record);
    }

    @Override
    @ActionEndpoint(PlatformAction.UPDATE)
    public WebRecordResponse<WorkflowDefinition> update(@Context UriInfo uriInfo,
                                                        @PathParam("id") String id,
                                                        WorkflowDefinition record) {
        requireDraft(requireScopedRecord(requestScope(uriInfo), id),
                "workflow definition can only edit draft definitions");
        normalizeDraft(record);
        return super.update(uriInfo, id, record);
    }

    @Override
    @ActionEndpoint(PlatformAction.DELETE)
    public WebCountResponse delete(@Context UriInfo uriInfo, @PathParam("id") String id) {
        requireDraft(requireScopedRecord(requestScope(uriInfo), id),
                "workflow definition can only delete draft definitions");
        return super.delete(uriInfo, id);
    }

    @POST
    @Path("/{definitionId}/versions/{versionId}/publish")
    @CustomActionEndpoint(value = "publishWorkflowDefinition", title = "发布工作流定义",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "definitionId")
    public WorkflowVersion publish(@Context UriInfo uriInfo,
                                   @PathParam("definitionId") String definitionId,
                                   @PathParam("versionId") String versionId) {
        return webScope(() -> {
            requireScopedRecord(requestScope(uriInfo), definitionId);
            return publishFacade.publish(definitionId, versionId, currentOperatorIdOrNull());
        });
    }

    @POST
    @Path("/{definitionId}/disable")
    @CustomActionEndpoint(value = "disableWorkflowDefinition", title = "停用工作流定义",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "definitionId")
    public WorkflowDefinition disableDefinition(@Context UriInfo uriInfo, @PathParam("definitionId") String definitionId) {
        return webScope(() -> {
            requireScopedRecord(requestScope(uriInfo), definitionId);
            return publishFacade.disable(definitionId);
        });
    }

    @POST
    @Path("/{definitionId}/archive")
    @CustomActionEndpoint(value = "archiveWorkflowDefinition", title = "归档工作流定义",
            level = PlatformActionLevel.RECORD, dataAuth = true, recordIdPathVariable = "definitionId")
    public WorkflowDefinition archive(@Context UriInfo uriInfo, @PathParam("definitionId") String definitionId) {
        return webScope(() -> {
            requireScopedRecord(requestScope(uriInfo), definitionId);
            return publishFacade.archive(definitionId);
        });
    }

    private String moduleAlias(WebRequestScope request) {
        return PlatformNameRules.requireModuleAlias(pathVariable(request, "moduleAlias"));
    }

    private PlatformModule requireModule(WebRequestScope request) {
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
