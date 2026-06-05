package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowApprovalTaskPolicyService {
    public boolean isNodePassed(WorkflowApprovalMode approvalMode, Integer approvalRatio, List<WorkflowTask> tasks) {
        WorkflowApprovalMode mode = approvalMode == null ? WorkflowApprovalMode.ANY : approvalMode;
        if (mode == WorkflowApprovalMode.NOTICE) {
            return true;
        }
        List<WorkflowTask> approvalTasks = approvalTasks(tasks);
        if (approvalTasks.isEmpty()) {
            return false;
        }
        long doneCount = approvalTasks.stream()
                .filter(task -> task.getTaskStatus() == WorkflowTaskStatus.DONE)
                .count();
        return switch (mode) {
            case ANY -> doneCount > 0;
            case ALL -> doneCount == approvalTasks.size();
            case RATIO -> ratioPassed(doneCount, approvalTasks.size(), approvalRatio);
            case NOTICE -> true;
        };
    }

    public boolean shouldSkipPendingSiblings(WorkflowApprovalMode approvalMode, Integer approvalRatio,
                                            List<WorkflowTask> tasks) {
        return isNodePassed(approvalMode, approvalRatio, tasks);
    }

    private List<WorkflowTask> approvalTasks(List<WorkflowTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream()
                .filter(task -> task.getTaskKind() == WorkflowTaskKind.APPROVAL)
                .filter(task -> task.getTaskStatus() == WorkflowTaskStatus.TODO
                        || task.getTaskStatus() == WorkflowTaskStatus.DONE)
                .toList();
    }

    private boolean ratioPassed(long doneCount, int totalCount, Integer approvalRatio) {
        int ratio = ratio(approvalRatio);
        return doneCount * 100 >= (long) totalCount * ratio;
    }

    private int ratio(Integer approvalRatio) {
        if (approvalRatio == null || approvalRatio <= 0 || approvalRatio > 100) {
            throw new PlatformException("workflow approval ratio must be between 1 and 100");
        }
        return approvalRatio;
    }
}
