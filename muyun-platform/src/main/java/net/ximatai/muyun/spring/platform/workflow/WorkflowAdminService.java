package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkflowAdminService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowInstanceDao instanceDao;
    private final WorkflowTaskDao taskDao;
    private final WorkflowActionPolicyService actionPolicyService;
    private final WorkflowInstanceActionService instanceActionService;
    private final WorkflowTaskActionService taskActionService;
    private final WorkflowHistoryQueryService historyQueryService;

    public WorkflowAdminService(WorkflowInstanceDao instanceDao,
                                WorkflowTaskDao taskDao,
                                WorkflowActionPolicyService actionPolicyService,
                                WorkflowInstanceActionService instanceActionService,
                                WorkflowTaskActionService taskActionService) {
        this(instanceDao, taskDao, actionPolicyService, instanceActionService, taskActionService, null);
    }

    @Autowired
    public WorkflowAdminService(WorkflowInstanceDao instanceDao,
                                WorkflowTaskDao taskDao,
                                WorkflowActionPolicyService actionPolicyService,
                                WorkflowInstanceActionService instanceActionService,
                                WorkflowTaskActionService taskActionService,
                                WorkflowHistoryQueryService historyQueryService) {
        this.instanceDao = instanceDao;
        this.taskDao = taskDao;
        this.actionPolicyService = actionPolicyService == null ? new WorkflowActionPolicyService() : actionPolicyService;
        this.instanceActionService = instanceActionService;
        this.taskActionService = taskActionService;
        this.historyQueryService = historyQueryService;
    }

    public List<WorkflowTask> currentTodoTasks(String instanceId) {
        WorkflowInstance instance = requireRunningInstance(instanceId);
        actionPolicyService.requireManagementAction(WorkflowActionPolicyService.MANAGEMENT_TODO_TASK_QUERY_ACTION);
        return taskDao.query(Criteria.of()
                        .eq("instanceId", instance.getId())
                        .eq("taskStatus", WorkflowTaskStatus.TODO),
                ALL, Sort.asc("createdAt"));
    }

    @Transactional
    public WorkflowInstanceActionResult forceTerminate(WorkflowInstanceActionRequest request) {
        return instanceActionService.forceTerminate(request);
    }

    @Transactional
    public WorkflowTaskActionResult forceApprove(WorkflowTaskActionRequest request) {
        return taskActionService.forceApprove(request);
    }

    public List<WorkflowHistoryInstance> queryHistory(String moduleAlias, String recordId, PageRequest pageRequest) {
        return historyQueryService.queryAdminHistory(moduleAlias, recordId, pageRequest);
    }

    public WorkflowRuntimeRenderBundle renderHistoryBundle(String historyInstanceId) {
        return historyQueryService.renderAdminBundle(historyInstanceId);
    }

    public List<WorkflowEvent> historyEvents(String historyInstanceId) {
        return historyQueryService.adminEvents(historyInstanceId);
    }

    public List<WorkflowHistoryEventView> historyEventViews(String historyInstanceId) {
        return historyQueryService.adminEventViews(historyInstanceId);
    }

    @Transactional
    public int deleteHistory(String historyInstanceId) {
        return historyQueryService.deleteHistory(historyInstanceId);
    }

    private WorkflowInstance requireRunningInstance(String instanceId) {
        WorkflowInstance instance = requireInstance(instanceId);
        if (instance.getInstanceStatus() != WorkflowInstanceStatus.RUNNING) {
            throw new PlatformException("workflow instance is not running: " + instance.getId());
        }
        return instance;
    }

    private WorkflowInstance requireInstance(String instanceId) {
        String validInstanceId = requireText(instanceId, "workflow instance id must not be blank");
        WorkflowInstance instance = instanceDao.findById(validInstanceId);
        if (instance == null) {
            throw new PlatformException("workflow instance not found: " + validInstanceId);
        }
        return instance;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
