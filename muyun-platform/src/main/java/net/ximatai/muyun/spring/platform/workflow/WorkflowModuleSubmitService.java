package net.ximatai.muyun.spring.platform.workflow;

import org.springframework.stereotype.Service;

@Service
public class WorkflowModuleSubmitService {
    private final WorkflowSubmitFacade submitFacade;

    public WorkflowModuleSubmitService(WorkflowSubmitFacade submitFacade) {
        this.submitFacade = submitFacade;
    }

    public WorkflowSubmitResult submitApproval(String moduleAlias, String recordId) {
        return submitFacade.submit(WorkflowSubmitRequest.approval(moduleAlias, recordId));
    }

    public WorkflowSubmitResult submitWorkflow(String moduleAlias, String recordId, String definitionAlias) {
        return submitFacade.submit(WorkflowSubmitRequest.workflow(moduleAlias, recordId, definitionAlias));
    }
}
