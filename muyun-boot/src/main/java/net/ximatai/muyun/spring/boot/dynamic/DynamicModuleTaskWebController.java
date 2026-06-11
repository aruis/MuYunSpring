package net.ximatai.muyun.spring.boot.dynamic;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionEndpoint;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContext;
import net.ximatai.muyun.spring.common.platform.ActionExecutionContextHolder;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformAction;
import net.ximatai.muyun.spring.common.tenant.ActiveTenantVerifier;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskCheckService;
import net.ximatai.muyun.spring.platform.ui.PlatformModuleTaskStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/{moduleAlias:[a-z][a-z0-9_]*(?:\\.[a-z][a-z0-9_]*)+}")
public class DynamicModuleTaskWebController {
    private final PlatformModuleTaskCheckService taskCheckService;
    private final DynamicRecordService recordService;
    private final ActiveTenantVerifier activeTenantVerifier;

    public DynamicModuleTaskWebController(PlatformModuleTaskCheckService taskCheckService,
                                          DynamicRecordService recordService,
                                          ActiveTenantVerifier activeTenantVerifier) {
        this.taskCheckService = taskCheckService;
        this.recordService = recordService;
        this.activeTenantVerifier = activeTenantVerifier;
    }

    @PostMapping("/view/{id}/tasks/check")
    @ActionEndpoint(PlatformAction.VIEW)
    public List<PlatformModuleTaskStatus> checkTasks(@PathVariable String id,
                                                      @RequestBody(required = false) DynamicModuleTaskCheckRequest request) {
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
