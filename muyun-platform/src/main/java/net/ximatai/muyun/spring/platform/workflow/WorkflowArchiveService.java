package net.ximatai.muyun.spring.platform.workflow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class WorkflowArchiveService {
    private static final PageRequest ALL = new PageRequest(0, Integer.MAX_VALUE);

    private final WorkflowInstanceDao instanceDao;
    private final WorkflowNodeInstanceDao nodeDao;
    private final WorkflowRouteInstanceDao routeDao;
    private final WorkflowTaskDao taskDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowHistoryInstanceDao historyDao;
    private final ObjectMapper objectMapper;

    public WorkflowArchiveService(WorkflowInstanceDao instanceDao,
                                  WorkflowNodeInstanceDao nodeDao,
                                  WorkflowRouteInstanceDao routeDao,
                                  WorkflowTaskDao taskDao,
                                  WorkflowEventDao eventDao,
                                  WorkflowHistoryInstanceDao historyDao) {
        this(instanceDao, nodeDao, routeDao, taskDao, eventDao, historyDao, defaultObjectMapper());
    }

    WorkflowArchiveService(WorkflowInstanceDao instanceDao,
                           WorkflowNodeInstanceDao nodeDao,
                           WorkflowRouteInstanceDao routeDao,
                           WorkflowTaskDao taskDao,
                           WorkflowEventDao eventDao,
                           WorkflowHistoryInstanceDao historyDao,
                           ObjectMapper objectMapper) {
        this.instanceDao = instanceDao;
        this.nodeDao = nodeDao;
        this.routeDao = routeDao;
        this.taskDao = taskDao;
        this.eventDao = eventDao;
        this.historyDao = historyDao;
        this.objectMapper = objectMapper == null ? defaultObjectMapper() : objectMapper;
    }

    @Transactional
    public WorkflowHistoryInstance archiveCurrentInstance(WorkflowInstance instance,
                                                          WorkflowArchiveReason archiveReason,
                                                          Instant archivedAt) {
        if (instance == null) {
            throw new PlatformException("workflow instance must not be null");
        }
        if (archiveReason == null) {
            throw new PlatformException("workflow archive reason must not be null");
        }
        Instant effectiveArchivedAt = archivedAt == null ? Instant.now() : archivedAt;
        List<WorkflowNodeInstance> nodes = nodes(instance.getId());
        List<WorkflowRouteInstance> routes = routes(instance.getId());
        List<WorkflowTask> tasks = tasks(instance.getId());
        List<WorkflowEvent> events = events(instance.getId());

        WorkflowHistorySnapshot snapshot = new WorkflowHistorySnapshot(1, effectiveArchivedAt, archiveReason,
                instance, nodes, routes, tasks, events);
        WorkflowHistoryInstance history = toHistoryInstance(instance, archiveReason, effectiveArchivedAt, snapshot);
        historyDao.insert(history);

        for (WorkflowEvent event : events) {
            eventDao.deleteById(event.getId());
        }
        for (WorkflowTask task : tasks) {
            taskDao.deleteById(task.getId());
        }
        for (WorkflowRouteInstance route : routes) {
            routeDao.deleteById(route.getId());
        }
        for (WorkflowNodeInstance node : nodes) {
            nodeDao.deleteById(node.getId());
        }
        instanceDao.deleteById(instance.getId());
        return history;
    }

    public WorkflowHistorySnapshot parseSnapshot(WorkflowHistoryInstance history) {
        if (history == null) {
            throw new PlatformException("workflow history instance must not be null");
        }
        try {
            return objectMapper.readValue(history.getSnapshotText(), WorkflowHistorySnapshot.class);
        } catch (JsonProcessingException ex) {
            throw new PlatformException("workflow history snapshot parse failed: " + history.getId(), ex);
        }
    }

    private WorkflowHistoryInstance toHistoryInstance(WorkflowInstance source,
                                                      WorkflowArchiveReason archiveReason,
                                                      Instant archivedAt,
                                                      WorkflowHistorySnapshot snapshot) {
        WorkflowHistoryInstance target = new WorkflowHistoryInstance();
        copyBase(source, target);
        target.setDefinitionId(source.getDefinitionId());
        target.setWorkflowVersionId(source.getWorkflowVersionId());
        target.setVersionNo(source.getVersionNo());
        target.setModuleAlias(source.getModuleAlias());
        target.setRecordId(source.getRecordId());
        target.setApprovalEnabled(source.getApprovalEnabled());
        target.setApprovalStatus(source.getApprovalStatus());
        target.setInstanceStatus(source.getInstanceStatus());
        target.setStartedBy(source.getStartedBy());
        target.setStartedAt(source.getStartedAt());
        target.setCompletedAt(source.getCompletedAt());
        target.setTerminatedAt(source.getTerminatedAt());
        target.setPreviousInstanceId(source.getPreviousInstanceId());
        target.setLastActionCode(source.getLastActionCode());
        target.setLastActionReason(source.getLastActionReason());
        target.setLastOperatorId(source.getLastOperatorId());
        target.setLastOperatedAt(source.getLastOperatedAt());
        target.setArchiveReason(archiveReason);
        target.setArchivedAt(archivedAt);
        target.setSnapshotText(writeSnapshot(snapshot, source.getId()));
        target.setSemanticJson(source.getSemanticJson());
        target.setLayoutJson(source.getLayoutJson());
        return target;
    }

    private String writeSnapshot(WorkflowHistorySnapshot snapshot, String instanceId) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new PlatformException("workflow history snapshot serialize failed: " + instanceId, ex);
        }
    }

    private List<WorkflowNodeInstance> nodes(String instanceId) {
        return nodeDao.query(Criteria.of().eq("instanceId", instanceId), ALL, Sort.asc("createdAt"));
    }

    private List<WorkflowRouteInstance> routes(String instanceId) {
        return routeDao.query(Criteria.of().eq("instanceId", instanceId), ALL, Sort.asc("createdAt"));
    }

    private List<WorkflowTask> tasks(String instanceId) {
        return taskDao.query(Criteria.of().eq("instanceId", instanceId), ALL, Sort.asc("createdAt"));
    }

    private List<WorkflowEvent> events(String instanceId) {
        return eventDao.query(Criteria.of().eq("instanceId", instanceId), ALL,
                Sort.asc("occurredAt"), Sort.asc("createdAt"));
    }

    private void copyBase(WorkflowInstance source, WorkflowHistoryInstance target) {
        target.setId(source.getId());
        target.setTenantId(source.getTenantId());
        target.setVersion(source.getVersion());
        target.setDeleted(source.getDeleted());
        target.setDeletedAt(source.getDeletedAt());
        target.setCreatedBy(source.getCreatedBy());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedBy(source.getUpdatedBy());
        target.setUpdatedAt(source.getUpdatedAt());
    }

    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
