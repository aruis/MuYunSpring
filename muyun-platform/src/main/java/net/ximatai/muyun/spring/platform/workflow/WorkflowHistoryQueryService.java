package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowHistoryQueryService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowHistoryInstanceDao historyDao;
    private final WorkflowArchiveService archiveService;
    private final WorkflowActionPolicyService actionPolicyService;

    public WorkflowHistoryQueryService(WorkflowHistoryInstanceDao historyDao,
                                       WorkflowArchiveService archiveService) {
        this(historyDao, archiveService, null);
    }

    @Autowired
    public WorkflowHistoryQueryService(WorkflowHistoryInstanceDao historyDao,
                                       WorkflowArchiveService archiveService,
                                       WorkflowActionPolicyService actionPolicyService) {
        this.historyDao = historyDao;
        this.archiveService = archiveService;
        this.actionPolicyService = actionPolicyService == null ? new WorkflowActionPolicyService() : actionPolicyService;
    }

    public List<WorkflowHistoryInstance> queryRecordHistory(String moduleAlias, String recordId,
                                                            PageRequest pageRequest) {
        return historyDao.query(Criteria.of()
                        .eq("moduleAlias", requireText(moduleAlias, "workflow module alias must not be blank"))
                        .eq("recordId", requireText(recordId, "workflow record id must not be blank")),
                page(pageRequest), Sort.desc("archivedAt"), Sort.desc("startedAt"));
    }

    public List<WorkflowHistoryInstance> queryAdminHistory(String moduleAlias, String recordId,
                                                           PageRequest pageRequest) {
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        Criteria criteria = Criteria.of()
                .eq("moduleAlias", requireText(moduleAlias, "workflow module alias must not be blank"));
        if (recordId != null && !recordId.isBlank()) {
            criteria.eq("recordId", recordId);
        }
        return historyDao.query(criteria, page(pageRequest), Sort.desc("archivedAt"), Sort.desc("startedAt"));
    }

    public WorkflowRuntimeRenderBundle renderAdminBundle(String historyInstanceId) {
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        return renderBundle(historyInstanceId);
    }

    public List<WorkflowEvent> adminEvents(String historyInstanceId) {
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        return events(historyInstanceId);
    }

    public List<WorkflowHistoryEventView> adminEventViews(String historyInstanceId) {
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        return eventViews(historyInstanceId);
    }

    @Transactional
    public int deleteHistory(String historyInstanceId) {
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_DELETE_HISTORY_ACTION);
        WorkflowHistoryInstance history = requireHistory(historyInstanceId);
        return historyDao.deleteById(history.getId());
    }

    public WorkflowRuntimeRenderBundle renderBundle(String historyInstanceId) {
        WorkflowHistoryInstance history = requireHistory(historyInstanceId);
        WorkflowHistorySnapshot snapshot = archiveService.parseSnapshot(history);
        return new WorkflowRuntimeRenderBundle("HISTORY", snapshot.instance(), snapshot.nodes(), snapshot.routes());
    }

    public List<WorkflowTask> tasks(String historyInstanceId) {
        return archiveService.parseSnapshot(requireHistory(historyInstanceId)).tasks();
    }

    public List<WorkflowHistoryTaskView> taskViews(String historyInstanceId) {
        return archiveService.parseSnapshot(requireHistory(historyInstanceId)).tasks().stream()
                .map(WorkflowHistoryTaskView::from)
                .toList();
    }

    public List<WorkflowEvent> events(String historyInstanceId) {
        return archiveService.parseSnapshot(requireHistory(historyInstanceId)).events();
    }

    public List<WorkflowHistoryEventView> eventViews(String historyInstanceId) {
        WorkflowHistorySnapshot snapshot = archiveService.parseSnapshot(requireHistory(historyInstanceId));
        Map<String, WorkflowTask> tasksById = snapshot.tasks().stream()
                .filter(task -> task.getId() != null)
                .collect(Collectors.toMap(WorkflowTask::getId, Function.identity(), (left, right) -> left));
        return snapshot.events().stream()
                .map(event -> WorkflowHistoryEventView.from(event, tasksById.get(event.getTaskId())))
                .toList();
    }

    private WorkflowHistoryInstance requireHistory(String historyInstanceId) {
        WorkflowHistoryInstance history = historyDao.findById(
                requireText(historyInstanceId, "workflow history instance id must not be blank"));
        if (history == null) {
            throw new PlatformException("workflow history instance not found: " + historyInstanceId);
        }
        return history;
    }

    private PageRequest page(PageRequest pageRequest) {
        return pageRequest == null ? ALL : pageRequest;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
