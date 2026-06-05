package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

@Service
public class WorkflowModuleTaskRuntimeService {
    private final WorkflowTaskDao taskDao;
    private final WorkflowInstanceDao instanceDao;
    private final WorkflowNodeInstanceDao nodeDao;
    private final WorkflowTaskDefinitionDao taskDefinitionDao;
    private final WorkflowModuleTaskEvaluator evaluator;
    private final WorkflowTaskActionFacade taskActionFacade;

    public WorkflowModuleTaskRuntimeService(WorkflowTaskDao taskDao,
                                            WorkflowInstanceDao instanceDao,
                                            WorkflowNodeInstanceDao nodeDao,
                                            WorkflowTaskDefinitionDao taskDefinitionDao,
                                            WorkflowModuleTaskEvaluator evaluator,
                                            WorkflowTaskActionFacade taskActionFacade) {
        this.taskDao = taskDao;
        this.instanceDao = instanceDao;
        this.nodeDao = nodeDao;
        this.taskDefinitionDao = taskDefinitionDao;
        this.evaluator = evaluator;
        this.taskActionFacade = taskActionFacade;
    }

    public WorkflowModuleTaskProcessBundle prepare(String taskId, String operatorId) {
        Context context = requireContext(taskId, operatorId);
        return bundle(context);
    }

    public WorkflowModuleTaskContinueResult checkAndContinue(String taskId, String operatorId, String reason) {
        Context context = requireContext(taskId, operatorId);
        WorkflowModuleTaskProcessBundle bundle = bundle(context);
        if (!bundle.evaluation().passed()) {
            return WorkflowModuleTaskContinueResult.blocked(bundle);
        }
        WorkflowTaskActionResult result = taskActionFacade.execute("complete",
                WorkflowTaskActionRequest.complete(taskId, operatorId, reason));
        return WorkflowModuleTaskContinueResult.continued(result);
    }

    private WorkflowModuleTaskProcessBundle bundle(Context context) {
        WorkflowModuleTaskEvaluation evaluation = evaluator.evaluate(context.instance(), context.node(), context.task(),
                context.taskDefinition());
        WorkflowModuleTaskCompletionPolicy policy = completionPolicy(context.taskDefinition(), evaluation);
        return new WorkflowModuleTaskProcessBundle(
                context.task().getId(),
                context.instance().getId(),
                context.node().getNodeKey(),
                context.instance().getModuleAlias(),
                context.instance().getRecordId(),
                policy,
                new WorkflowModuleTaskContext(context.task().getId(), policy,
                        "/workflow/runtime/task/" + context.task().getId() + "/module-task/check-and-continue"),
                context.taskDefinition(),
                evaluation,
                evaluation.guides().isEmpty() ? null : evaluation.guides().getFirst()
        );
    }

    private WorkflowModuleTaskCompletionPolicy completionPolicy(WorkflowTaskDefinition definition,
                                                               WorkflowModuleTaskEvaluation evaluation) {
        if (evaluation.passed() && evaluation.checkStatus() == WorkflowTaskCheckStatus.PASSED) {
            return WorkflowModuleTaskCompletionPolicy.CHECK_PASSED;
        }
        if (!evaluation.guides().isEmpty()
                && evaluation.guides().getFirst().getGuideKind() == WorkflowTaskGuideKind.EXECUTE_ACTION) {
            return WorkflowModuleTaskCompletionPolicy.AFTER_ACTION_SUCCESS;
        }
        if (Boolean.TRUE.equals(definition.getManualConfirm())) {
            return WorkflowModuleTaskCompletionPolicy.MANUAL_CONFIRM;
        }
        return WorkflowModuleTaskCompletionPolicy.CHECK_PASSED;
    }

    private Context requireContext(String taskId, String operatorId) {
        WorkflowTask task = requireTask(taskId);
        if (task.getTaskKind() != WorkflowTaskKind.BUSINESS) {
            throw new PlatformException("workflow task is not a business task: " + taskId);
        }
        if (task.getTaskStatus() != WorkflowTaskStatus.TODO) {
            throw new PlatformException("workflow task is not todo: " + taskId);
        }
        if (operatorId == null || operatorId.isBlank() || !operatorId.equals(task.getAssigneeId())) {
            throw new PlatformException("workflow task action operator is not assignee: " + taskId);
        }
        WorkflowInstance instance = requireInstance(task);
        if (instance.getInstanceStatus() != WorkflowInstanceStatus.RUNNING) {
            throw new PlatformException("workflow instance is not running: " + instance.getId());
        }
        WorkflowNodeInstance node = requireNode(task);
        if (node.getNodeType() != WorkflowNodeType.TASK || node.getNodeStatus() != WorkflowNodeStatus.ACTIVE) {
            throw new PlatformException("workflow node is not active task node: " + node.getNodeKey());
        }
        WorkflowTaskDefinition definition = requireTaskDefinition(node);
        return new Context(instance, node, task, definition);
    }

    private WorkflowTask requireTask(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new PlatformException("workflow task id must not be blank");
        }
        WorkflowTask task = taskDao.findById(taskId);
        if (task == null) {
            throw new PlatformException("workflow task not found: " + taskId);
        }
        return task;
    }

    private WorkflowInstance requireInstance(WorkflowTask task) {
        WorkflowInstance instance = instanceDao.findById(task.getInstanceId());
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + task.getInstanceId());
        }
        return instance;
    }

    private WorkflowNodeInstance requireNode(WorkflowTask task) {
        WorkflowNodeInstance node = nodeDao.findById(task.getNodeInstanceId());
        if (node == null) {
            throw new PlatformException("workflow node instance not found: " + task.getNodeInstanceId());
        }
        return node;
    }

    private WorkflowTaskDefinition requireTaskDefinition(WorkflowNodeInstance node) {
        String taskDefinitionId = node.getTaskDefinitionId();
        if (taskDefinitionId == null || taskDefinitionId.isBlank()) {
            throw new PlatformException("workflow task node missing task definition: " + node.getNodeKey());
        }
        WorkflowTaskDefinition definition = taskDefinitionDao.findById(taskDefinitionId);
        if (definition == null || !Boolean.TRUE.equals(definition.getEnabled())) {
            throw new PlatformException("workflow task definition not found or disabled: " + taskDefinitionId);
        }
        return definition;
    }

    private record Context(WorkflowInstance instance,
                           WorkflowNodeInstance node,
                           WorkflowTask task,
                           WorkflowTaskDefinition taskDefinition) {
    }
}
