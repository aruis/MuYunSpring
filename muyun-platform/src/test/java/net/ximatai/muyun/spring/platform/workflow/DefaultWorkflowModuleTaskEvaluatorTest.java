package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultWorkflowModuleTaskEvaluatorTest {
    private final WorkflowTaskCheckResultDao checkResultDao = mock(WorkflowTaskCheckResultDao.class);
    private final WorkflowTaskGuideDao guideDao = mock(WorkflowTaskGuideDao.class);
    private final DefaultWorkflowModuleTaskEvaluator evaluator =
            new DefaultWorkflowModuleTaskEvaluator(checkResultDao, guideDao);

    @Test
    void shouldTreatManualConfirmTaskAsPassedWithoutChecks() {
        WorkflowTask task = task(WorkflowTaskCheckStatus.NOT_CHECKED);
        WorkflowTaskDefinition definition = definition(true);
        WorkflowTaskGuide guide = guide();
        when(guideDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of(guide));

        WorkflowModuleTaskEvaluation evaluation = evaluator.evaluate(instance(), node(), task, definition);

        assertThat(evaluation.passed()).isTrue();
        assertThat(evaluation.checkStatus()).isEqualTo(WorkflowTaskCheckStatus.NO_CHECK);
        assertThat(evaluation.guides()).containsExactly(guide);
    }

    @Test
    void shouldPassWhenAllCheckResultsPassed() {
        WorkflowTask task = task(WorkflowTaskCheckStatus.NOT_CHECKED);
        WorkflowTaskDefinition definition = definition(false);
        WorkflowTaskCheckResult result = checkResult(true, WorkflowTaskCheckStatus.PASSED, null);
        when(guideDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of());
        when(checkResultDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class)))
                .thenReturn(List.of(result));

        WorkflowModuleTaskEvaluation evaluation = evaluator.evaluate(instance(), node(), task, definition);

        assertThat(evaluation.passed()).isTrue();
        assertThat(evaluation.checkResults()).containsExactly(result);
    }

    @Test
    void shouldFailWhenAnyCheckResultFails() {
        WorkflowTask task = task(WorkflowTaskCheckStatus.NOT_CHECKED);
        WorkflowTaskDefinition definition = definition(false);
        WorkflowTaskCheckResult result = checkResult(false, WorkflowTaskCheckStatus.FAILED, "missing contract");
        when(guideDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class), any(Sort.class)))
                .thenReturn(List.of());
        when(checkResultDao.query(any(Criteria.class), any(PageRequest.class), any(Sort.class)))
                .thenReturn(List.of(result));

        WorkflowModuleTaskEvaluation evaluation = evaluator.evaluate(instance(), node(), task, definition);

        assertThat(evaluation.passed()).isFalse();
        assertThat(evaluation.failureMessage()).isEqualTo("missing contract");
    }

    private WorkflowInstance instance() {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId("instance-1");
        return instance;
    }

    private WorkflowNodeInstance node() {
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId("node-1");
        return node;
    }

    private WorkflowTask task(WorkflowTaskCheckStatus status) {
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        task.setCheckStatus(status);
        return task;
    }

    private WorkflowTaskDefinition definition(boolean manualConfirm) {
        WorkflowTaskDefinition definition = new WorkflowTaskDefinition();
        definition.setId("task-def-1");
        definition.setManualConfirm(manualConfirm);
        return definition;
    }

    private WorkflowTaskGuide guide() {
        WorkflowTaskGuide guide = new WorkflowTaskGuide();
        guide.setId("guide-1");
        guide.setGuideKind(WorkflowTaskGuideKind.EXECUTE_ACTION);
        return guide;
    }

    private WorkflowTaskCheckResult checkResult(boolean passed, WorkflowTaskCheckStatus status, String message) {
        WorkflowTaskCheckResult result = new WorkflowTaskCheckResult();
        result.setPassed(passed);
        result.setCheckStatus(status);
        result.setFailureMessage(message);
        return result;
    }
}
