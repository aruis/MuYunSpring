package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowTaskActionAvailabilityService {
    private final WorkflowTaskDao taskDao;
    private final WorkflowInstanceDao instanceDao;
    private final WorkflowNodeInstanceDao nodeDao;

    public WorkflowTaskActionAvailabilityService(WorkflowTaskDao taskDao,
                                                 WorkflowInstanceDao instanceDao,
                                                 WorkflowNodeInstanceDao nodeDao) {
        this.taskDao = taskDao;
        this.instanceDao = instanceDao;
        this.nodeDao = nodeDao;
    }

    public List<WorkflowTaskAvailableAction> availableActions(String taskId, String operatorId) {
        WorkflowTask task = requireTask(taskId);
        if (task.getTaskStatus() != WorkflowTaskStatus.TODO || !canOperate(task, operatorId)) {
            return List.of();
        }
        WorkflowInstance instance = requireInstance(task);
        WorkflowNodeInstance node = task.getNodeInstanceId() == null ? null : nodeDao.findById(task.getNodeInstanceId());
        List<WorkflowTaskAvailableAction> actions = new ArrayList<>();
        if (task.getTaskKind() == WorkflowTaskKind.APPROVAL) {
            actions.add(WorkflowTaskAvailableAction.of("approve", "通过"));
            if (node != null && !Boolean.FALSE.equals(node.getAllowReject())) {
                actions.add(WorkflowTaskAvailableAction.of("reject", "驳回")
                        .requireReason(Boolean.TRUE.equals(node.getRequireRejectReason()))
                        .supportRejectReturnToMe(Boolean.TRUE.equals(node.getAllowRejectReturnToMe())));
            }
            if (instance.getInstanceStatus() == WorkflowInstanceStatus.RUNNING
                    && node != null
                    && Boolean.TRUE.equals(node.getAllowRollback())) {
                actions.add(WorkflowTaskAvailableAction.of("rollback", "回退")
                        .requireReason(Boolean.TRUE.equals(node.getRequireRollbackReason())));
            }
        } else if (task.getTaskKind() == WorkflowTaskKind.BUSINESS) {
            actions.add(WorkflowTaskAvailableAction.of("complete", "完成"));
        } else if (task.getTaskKind() == WorkflowTaskKind.NOTICE) {
            actions.add(WorkflowTaskAvailableAction.of("notice", "已阅"));
        } else if (task.getTaskKind() == WorkflowTaskKind.RESUBMIT
                && instance.getInstanceStatus() == WorkflowInstanceStatus.REJECTED
                && instance.getRejectResubmitMode() == WorkflowRejectResubmitMode.RETURN_TO_ME) {
            actions.add(WorkflowTaskAvailableAction.of("resubmit", "重提"));
        }
        if (task.getTaskKind() != WorkflowTaskKind.RESUBMIT) {
            actions.add(WorkflowTaskAvailableAction.of("transfer", "转办").requireTargetAssignee());
        }
        return List.copyOf(actions);
    }

    private WorkflowTask requireTask(String taskId) {
        String validTaskId = requireText(taskId, "workflow task id must not be blank");
        WorkflowTask task = taskDao.findById(validTaskId);
        if (task == null) {
            throw new PlatformException("workflow task not found: " + validTaskId);
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

    private boolean canOperate(WorkflowTask task, String operatorId) {
        String validOperatorId = requireText(operatorId, "workflow operator id must not be blank");
        if (CurrentUserContext.currentUser()
                .filter(user -> user.system() && validOperatorId.equals(user.userId()))
                .isPresent()) {
            return true;
        }
        return validOperatorId.equals(task.getAssigneeId());
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
