package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.iam.tenant.TenantService;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskCheckResult;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskCheckService;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskDefinition;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;

import java.util.List;

@ApplicationScoped
@Path("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}")
public class DynamicModuleTaskWebController {
    private final PlatformModuleTaskCheckService taskCheckService;
    private final DynamicRecordService recordService;
    private final TenantService activeTenantVerifier;

    public DynamicModuleTaskWebController(PlatformModuleTaskCheckService taskCheckService,
                                          DynamicRecordService recordService,
                                          TenantService activeTenantVerifier) {
        this.taskCheckService = taskCheckService;
        this.recordService = recordService;
        this.activeTenantVerifier = activeTenantVerifier;
    }

    @POST
    @Path("/view/{id}/tasks/check")
    @ActionEndpoint(PlatformAction.VIEW)
    public List<PlatformModuleTaskStatus> checkTasks(@PathParam("id") String id,
                                                     DynamicModuleTaskCheckRequest request) {
        return evaluateTasks(id, request).tasks();
    }

    @GET
    @Path("/tasks/definitions")
    @ActionEndpoint(PlatformAction.VIEW)
    public List<PlatformModuleTaskDefinition> taskDefinitions() {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        requireTenantContext(moduleAlias);
        return taskCheckService.definitions(moduleAlias);
    }

    @POST
    @Path("/view/{id}/tasks/evaluate")
    @ActionEndpoint(PlatformAction.VIEW)
    public PlatformModuleTaskCheckResult evaluateTasks(@PathParam("id") String id,
                                                       DynamicModuleTaskCheckRequest request) {
        String moduleAlias = DynamicWebRequest.moduleAlias();
        requireTenantContext(moduleAlias);
        requireDataScopeRecord(moduleAlias, id);
        String uiConfigId = request == null ? null : request.uiConfigId();
        return taskCheckService.check(moduleAlias, id, uiConfigId);
    }

    private void requireTenantContext(String moduleAlias) {
        String tenantId = TenantContext.currentTenantId()
                .orElseThrow(() -> new PlatformException(moduleAlias + " requires tenant context"));
        activeTenantVerifier.verifyActiveTenant(tenantId);
    }

    private void requireDataScopeRecord(String moduleAlias, String id) {
        recordService.requireRecordActionScope(moduleAlias, recordService.mainEntityAlias(moduleAlias),
                actionPolicy(moduleAlias), java.util.List.of(id), CurrentUserContext.currentUser());
    }

    private ActionExecutionPolicy actionPolicy(String moduleAlias) {
        return ActionExecutionContextHolder.current()
                .filter(context -> context.moduleAlias().equals(moduleAlias))
                .map(ActionExecutionContext::actionPolicy)
                .orElseGet(PlatformAction.VIEW::executionPolicy);
    }
}
