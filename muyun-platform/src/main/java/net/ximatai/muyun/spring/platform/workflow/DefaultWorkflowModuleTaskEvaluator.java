package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DefaultWorkflowModuleTaskEvaluator implements WorkflowModuleTaskEvaluator {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowTaskCheckResultDao checkResultDao;
    private final WorkflowTaskGuideDao guideDao;

    public DefaultWorkflowModuleTaskEvaluator(WorkflowTaskCheckResultDao checkResultDao,
                                              WorkflowTaskGuideDao guideDao) {
        this.checkResultDao = checkResultDao;
        this.guideDao = guideDao;
    }

    @Override
    public WorkflowModuleTaskEvaluation evaluate(WorkflowInstance instance,
                                                 WorkflowNodeInstance node,
                                                 WorkflowTask task,
                                                 WorkflowTaskDefinition taskDefinition) {
        List<WorkflowTaskGuide> guides = guideDao.query(Criteria.of()
                        .eq("taskDefinitionId", taskDefinition.getId())
                        .eq("enabled", true),
                ALL, Sort.asc("sortOrder"), Sort.asc("createdAt"));
        if (task.getCheckStatus() == WorkflowTaskCheckStatus.NO_CHECK
                || Boolean.TRUE.equals(taskDefinition.getManualConfirm())) {
            return WorkflowModuleTaskEvaluation.manualConfirm(guides);
        }
        List<WorkflowTaskCheckResult> results = checkResultDao.query(Criteria.of().eq("taskId", task.getId()),
                ALL, Sort.asc("createdAt"));
        if (results.isEmpty()) {
            return WorkflowModuleTaskEvaluation.failed("workflow module task check result is empty", results, guides);
        }
        boolean allPassed = results.stream().allMatch(result -> Boolean.TRUE.equals(result.getPassed())
                || result.getCheckStatus() == WorkflowTaskCheckStatus.PASSED);
        if (allPassed) {
            return WorkflowModuleTaskEvaluation.passed(results, guides);
        }
        String message = results.stream()
                .map(WorkflowTaskCheckResult::getFailureMessage)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("workflow module task check failed");
        return WorkflowModuleTaskEvaluation.failed(message, results, guides);
    }
}
