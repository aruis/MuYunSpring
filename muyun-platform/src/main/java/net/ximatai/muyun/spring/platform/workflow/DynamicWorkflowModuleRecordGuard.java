package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DynamicWorkflowModuleRecordGuard implements WorkflowModuleRecordGuard {
    public static final String SUBMIT_ACTION_CODE = "submit";
    private static final ActionExecutionPolicy SUBMIT_POLICY = new ActionExecutionPolicy(
            SUBMIT_ACTION_CODE,
            PlatformActionLevel.RECORD,
            ActionAccessMode.AUTH_REQUIRED,
            true,
            true,
            ActionDefaultGrantPolicy.NONE,
            null
    );

    private final DynamicRecordService dynamicRecordService;

    public DynamicWorkflowModuleRecordGuard(DynamicRecordService dynamicRecordService) {
        this.dynamicRecordService = dynamicRecordService;
    }

    @Override
    public void beforeSubmit(WorkflowSubmitRequest request) {
        String entityAlias = mainEntityAliasOrNull(request.moduleAlias());
        if (entityAlias == null) {
            return;
        }
        dynamicRecordService.requireRecordActionScope(request.moduleAlias(), entityAlias, SUBMIT_POLICY,
                Set.of(request.recordId()), CurrentUserContext.currentUser());
        if (dynamicRecordService.selectSystem(request.moduleAlias(), entityAlias, request.recordId()) == null) {
            throw new PlatformException("dynamic record not found: " + request.moduleAlias() + "." + request.recordId());
        }
    }

    private String mainEntityAliasOrNull(String moduleAlias) {
        try {
            return dynamicRecordService.mainEntityAlias(moduleAlias);
        } catch (ModuleDefinitionException ignored) {
            return null;
        }
    }
}
