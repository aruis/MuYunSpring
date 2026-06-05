package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowAdminFacade {
    private final WorkflowAdminService adminService;

    public WorkflowAdminFacade(WorkflowAdminService adminService) {
        this.adminService = adminService;
    }

    public List<WorkflowTask> currentTodoTasks(String instanceId) {
        return adminService.currentTodoTasks(instanceId);
    }

    public WorkflowInstanceActionResult forceTerminate(WorkflowInstanceActionRequest request) {
        return adminService.forceTerminate(request);
    }

    public WorkflowTaskActionResult forceApprove(WorkflowTaskActionRequest request) {
        return adminService.forceApprove(request);
    }

    public List<WorkflowHistoryInstance> queryHistory(String moduleAlias, String recordId, PageRequest pageRequest) {
        return adminService.queryHistory(moduleAlias, recordId, pageRequest);
    }

    public WorkflowRuntimeRenderBundle renderHistoryBundle(String historyInstanceId) {
        return adminService.renderHistoryBundle(historyInstanceId);
    }

    public List<WorkflowEvent> historyEvents(String historyInstanceId) {
        return adminService.historyEvents(historyInstanceId);
    }

    public List<WorkflowHistoryEventView> historyEventViews(String historyInstanceId) {
        return adminService.historyEventViews(historyInstanceId);
    }

    public int deleteHistory(String historyInstanceId) {
        return adminService.deleteHistory(historyInstanceId);
    }
}
