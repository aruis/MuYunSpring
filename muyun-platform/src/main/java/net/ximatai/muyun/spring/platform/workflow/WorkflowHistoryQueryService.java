package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkflowHistoryQueryService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowHistoryInstanceDao historyDao;
    private final WorkflowArchiveService archiveService;
    private final WorkflowActionPolicyService actionPolicyService;
    private final WorkflowUserTitleResolver userTitleResolver;

    public WorkflowHistoryQueryService(WorkflowHistoryInstanceDao historyDao,
                                       WorkflowArchiveService archiveService) {
        this(historyDao, archiveService, null, WorkflowUserTitleResolver.NONE);
    }

    @Autowired
    public WorkflowHistoryQueryService(WorkflowHistoryInstanceDao historyDao,
                                       WorkflowArchiveService archiveService,
                                       WorkflowActionPolicyService actionPolicyService,
                                       ObjectProvider<WorkflowUserTitleResolver> userTitleResolver) {
        this(historyDao, archiveService, actionPolicyService, userTitleResolver == null
                ? WorkflowUserTitleResolver.NONE
                : userTitleResolver.getIfAvailable(() -> WorkflowUserTitleResolver.NONE));
    }

    public WorkflowHistoryQueryService(WorkflowHistoryInstanceDao historyDao,
                                       WorkflowArchiveService archiveService,
                                       WorkflowActionPolicyService actionPolicyService) {
        this(historyDao, archiveService, actionPolicyService, WorkflowUserTitleResolver.NONE);
    }

    public WorkflowHistoryQueryService(WorkflowHistoryInstanceDao historyDao,
                                       WorkflowArchiveService archiveService,
                                       WorkflowActionPolicyService actionPolicyService,
                                       WorkflowUserTitleResolver userTitleResolver) {
        this.historyDao = historyDao;
        this.archiveService = archiveService;
        this.actionPolicyService = actionPolicyService == null ? new WorkflowActionPolicyService() : actionPolicyService;
        this.userTitleResolver = userTitleResolver == null ? WorkflowUserTitleResolver.NONE : userTitleResolver;
    }

    public List<WorkflowHistoryInstance> queryRecordHistory(String moduleAlias, String recordId,
                                                            PageRequest pageRequest) {
        return queryRecordHistory(moduleAlias, recordId, null, pageRequest);
    }

    public List<WorkflowHistoryInstance> queryRecordHistory(String moduleAlias, String recordId,
                                                            String startedBy,
                                                            PageRequest pageRequest) {
        Criteria criteria = Criteria.of()
                .eq("moduleAlias", requireText(moduleAlias, "workflow module alias must not be blank"))
                .eq("recordId", requireText(recordId, "workflow record id must not be blank"));
        if (startedBy != null && !startedBy.isBlank()) {
            criteria.eq("startedBy", startedBy);
        }
        return historyDao.query(criteria, page(pageRequest), Sort.desc("archivedAt"), Sort.desc("startedAt"));
    }

    public List<WorkflowHistoryInstance> queryAdminHistory(String moduleAlias, String recordId,
                                                           PageRequest pageRequest) {
        return queryAdminHistory(moduleAlias, recordId, null, pageRequest);
    }

    public List<WorkflowHistoryInstance> queryAdminHistory(String moduleAlias, String recordId,
                                                           String startedBy,
                                                           PageRequest pageRequest) {
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_QUERY_ACTION);
        Criteria criteria = Criteria.of()
                .eq("moduleAlias", requireText(moduleAlias, "workflow module alias must not be blank"));
        if (recordId != null && !recordId.isBlank()) {
            criteria.eq("recordId", recordId);
        }
        if (startedBy != null && !startedBy.isBlank()) {
            criteria.eq("startedBy", startedBy);
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
        List<WorkflowTask> tasks = archiveService.parseSnapshot(requireHistory(historyInstanceId)).tasks();
        Map<String, String> userTitles = userTitles(tasks, List.of());
        return tasks.stream()
                .map(task -> WorkflowHistoryTaskView.from(task, userTitles))
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
        Map<String, WorkflowNodeInstance> nodesById = snapshot.nodes().stream()
                .filter(node -> node.getId() != null)
                .collect(Collectors.toMap(WorkflowNodeInstance::getId, Function.identity(), (left, right) -> left));
        Map<String, WorkflowNodeInstance> nodesByKey = snapshot.nodes().stream()
                .filter(node -> node.getNodeKey() != null)
                .collect(Collectors.toMap(WorkflowNodeInstance::getNodeKey, Function.identity(), (left, right) -> left));
        Map<String, WorkflowRouteInstance> routesByIdOrKey = routesByIdOrKey(snapshot.routes());
        Map<String, String> userTitles = userTitles(snapshot.tasks(), snapshot.events());
        return snapshot.events().stream()
                .map(event -> WorkflowHistoryEventView.from(event, tasksById.get(event.getTaskId()),
                        nodesById, nodesByKey, routesByIdOrKey, userTitles))
                .toList();
    }

    private Map<String, String> userTitles(List<WorkflowTask> tasks, List<WorkflowEvent> events) {
        Set<String> userIds = new LinkedHashSet<>();
        for (WorkflowTask task : tasks == null ? List.<WorkflowTask>of() : tasks) {
            addUserId(userIds, task.getAssigneeId());
            addUserId(userIds, task.getOriginalAssigneeId());
            addUserId(userIds, task.getActualProcessorId());
            addUserId(userIds, task.getDelegatedFromUserId());
            addUserId(userIds, task.getDelegatedToUserId());
            addUserId(userIds, task.getTransferredFromUserId());
            addUserId(userIds, task.getTransferredBy());
        }
        for (WorkflowEvent event : events == null ? List.<WorkflowEvent>of() : events) {
            addUserId(userIds, event.getOperatorId());
        }
        Map<String, String> titles = userTitleResolver.titles(userIds);
        return titles == null ? Map.of() : titles;
    }

    private void addUserId(Set<String> userIds, String userId) {
        if (userId != null && !userId.isBlank()) {
            userIds.add(userId);
        }
    }

    private Map<String, WorkflowRouteInstance> routesByIdOrKey(List<WorkflowRouteInstance> routes) {
        Map<String, WorkflowRouteInstance> result = new LinkedHashMap<>();
        for (WorkflowRouteInstance route : routes) {
            if (route.getId() != null) {
                result.putIfAbsent(route.getId(), route);
            }
            if (route.getRouteKey() != null) {
                result.putIfAbsent(route.getRouteKey(), route);
            }
        }
        return result;
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
