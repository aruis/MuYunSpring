package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowTaskQueryService {
    private final WorkflowTaskDao taskDao;
    private final WorkflowEventDao eventDao;

    public WorkflowTaskQueryService(WorkflowTaskDao taskDao, WorkflowEventDao eventDao) {
        this.taskDao = taskDao;
        this.eventDao = eventDao;
    }

    public List<WorkflowTask> myTodo(String assigneeId, PageRequest pageRequest) {
        return taskDao.query(Criteria.of()
                        .eq("assigneeId", requireText(assigneeId, "workflow assignee id must not be blank"))
                        .eq("taskStatus", WorkflowTaskStatus.TODO),
                page(pageRequest), Sort.asc("dueAt"), Sort.desc("createdAt"));
    }

    public List<WorkflowTask> myDone(String processorId, PageRequest pageRequest) {
        return taskDao.query(Criteria.of()
                        .eq("actualProcessorId", requireText(processorId, "workflow processor id must not be blank"))
                        .in("taskStatus", List.of(WorkflowTaskStatus.DONE, WorkflowTaskStatus.REJECTED,
                                WorkflowTaskStatus.NOTICED, WorkflowTaskStatus.TRANSFERRED)),
                page(pageRequest), Sort.desc("completedAt"), Sort.desc("updatedAt"));
    }

    public List<WorkflowTask> myNotice(String assigneeId, PageRequest pageRequest) {
        return taskDao.query(Criteria.of()
                        .eq("assigneeId", requireText(assigneeId, "workflow assignee id must not be blank"))
                        .eq("taskKind", WorkflowTaskKind.NOTICE)
                        .in("taskStatus", List.of(WorkflowTaskStatus.TODO, WorkflowTaskStatus.NOTICED)),
                page(pageRequest), Sort.desc("createdAt"));
    }

    public List<WorkflowTask> instanceTasks(String instanceId, PageRequest pageRequest) {
        return taskDao.query(Criteria.of()
                        .eq("instanceId", requireText(instanceId, "workflow instance id must not be blank")),
                page(pageRequest), Sort.asc("createdAt"));
    }

    public List<WorkflowEvent> instanceEvents(String instanceId, PageRequest pageRequest) {
        return eventDao.query(Criteria.of()
                        .eq("instanceId", requireText(instanceId, "workflow instance id must not be blank")),
                page(pageRequest), Sort.asc("occurredAt"), Sort.asc("createdAt"));
    }

    private PageRequest page(PageRequest pageRequest) {
        return pageRequest == null ? PageRequest.of(1, 20) : pageRequest;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
        return value;
    }
}
