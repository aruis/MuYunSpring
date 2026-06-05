package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.platform.ActionExecutionPolicy;

public interface WorkflowModuleRecordGuard {
    void beforeSubmit(WorkflowSubmitRequest request);

    default void requireRecordAction(String moduleAlias, String recordId, ActionExecutionPolicy policy) {
    }
}
