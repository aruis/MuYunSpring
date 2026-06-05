package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.platform.ActionAccessMode;
import net.ximatai.muyun.spring.common.platform.ActionDefaultGrantPolicy;
import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;
import net.ximatai.muyun.spring.common.platform.PlatformActionLevel;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicRecordService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class DynamicWorkflowSubmitService {
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
    private final WorkflowSubmitFacade submitFacade;

    public DynamicWorkflowSubmitService(DynamicRecordService dynamicRecordService,
                                        WorkflowSubmitFacade submitFacade) {
        this.dynamicRecordService = dynamicRecordService;
        this.submitFacade = submitFacade;
    }

    public WorkflowSubmitResult submitApproval(String moduleAlias, String recordId) {
        requireMainRecordSubmitScope(moduleAlias, recordId);
        return submitFacade.submit(WorkflowSubmitRequest.approval(moduleAlias, recordId));
    }

    public WorkflowSubmitResult submitWorkflow(String moduleAlias, String recordId, String definitionAlias) {
        requireMainRecordSubmitScope(moduleAlias, recordId);
        return submitFacade.submit(WorkflowSubmitRequest.workflow(moduleAlias, recordId, definitionAlias));
    }

    private void requireMainRecordSubmitScope(String moduleAlias, String recordId) {
        String entityAlias = dynamicRecordService.mainEntityAlias(moduleAlias);
        dynamicRecordService.requireRecordActionScope(moduleAlias, entityAlias, SUBMIT_POLICY, Set.of(recordId),
                CurrentUserContext.currentUser());
        if (dynamicRecordService.selectSystem(moduleAlias, entityAlias, recordId) == null) {
            throw new PlatformException("dynamic record not found: " + moduleAlias + "." + recordId);
        }
    }
}
