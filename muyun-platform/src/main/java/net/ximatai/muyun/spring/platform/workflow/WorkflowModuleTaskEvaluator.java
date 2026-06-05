package net.ximatai.muyun.spring.platform.workflow;

public interface WorkflowModuleTaskEvaluator {
    WorkflowModuleTaskEvaluation evaluate(WorkflowInstance instance,
                                          WorkflowNodeInstance node,
                                          WorkflowTask task,
                                          WorkflowTaskDefinition taskDefinition);
}
