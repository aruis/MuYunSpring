package net.ximatai.muyun.spring.platform.workflow;

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
}
