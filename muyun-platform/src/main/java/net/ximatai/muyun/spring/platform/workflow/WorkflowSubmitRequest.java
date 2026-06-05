package net.ximatai.muyun.spring.platform.workflow;

import java.time.Instant;

public record WorkflowSubmitRequest(
        String moduleAlias,
        String recordId,
        boolean approvalRequired,
        String definitionAlias,
        String authOrgId,
        String operatorId,
        Instant operatedAt
) {
    public WorkflowSubmitRequest(String moduleAlias,
                                 String recordId,
                                 boolean approvalRequired,
                                 String definitionAlias,
                                 String operatorId,
                                 Instant operatedAt) {
        this(moduleAlias, recordId, approvalRequired, definitionAlias, null, operatorId, operatedAt);
    }

    public static WorkflowSubmitRequest approval(String moduleAlias, String recordId) {
        return new WorkflowSubmitRequest(moduleAlias, recordId, true, null, null, null, null);
    }

    public static WorkflowSubmitRequest workflow(String moduleAlias, String recordId, String definitionAlias) {
        return new WorkflowSubmitRequest(moduleAlias, recordId, false, definitionAlias, null, null, null);
    }

    public WorkflowSubmitRequest withOperator(String operatorId) {
        return new WorkflowSubmitRequest(moduleAlias, recordId, approvalRequired, definitionAlias, authOrgId,
                operatorId, operatedAt);
    }

    public WorkflowSubmitRequest withAuthOrgId(String authOrgId) {
        return new WorkflowSubmitRequest(moduleAlias, recordId, approvalRequired, definitionAlias, authOrgId,
                operatorId, operatedAt);
    }

    public WorkflowSubmitRequest withOperatedAt(Instant operatedAt) {
        return new WorkflowSubmitRequest(moduleAlias, recordId, approvalRequired, definitionAlias, authOrgId,
                operatorId, operatedAt);
    }
}
