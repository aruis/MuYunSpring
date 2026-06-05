package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class WorkflowRuntimeSubmitService {
    private final WorkflowSubmitDraftService submitDraftService;
    private final WorkflowInstanceService instanceService;
    private final WorkflowInstanceDao instanceDao;
    private final WorkflowNodeInstanceDao nodeInstanceDao;
    private final WorkflowRouteInstanceDao routeInstanceDao;
    private final WorkflowTaskDao taskDao;
    private final WorkflowEventDao eventDao;

    public WorkflowRuntimeSubmitService(WorkflowSubmitDraftService submitDraftService,
                                        WorkflowInstanceService instanceService,
                                        WorkflowInstanceDao instanceDao,
                                        WorkflowNodeInstanceDao nodeInstanceDao,
                                        WorkflowRouteInstanceDao routeInstanceDao,
                                        WorkflowTaskDao taskDao,
                                        WorkflowEventDao eventDao) {
        this.submitDraftService = submitDraftService;
        this.instanceService = instanceService;
        this.instanceDao = instanceDao;
        this.nodeInstanceDao = nodeInstanceDao;
        this.routeInstanceDao = routeInstanceDao;
        this.taskDao = taskDao;
        this.eventDao = eventDao;
    }

    @Transactional
    public WorkflowSubmitDraft submit(WorkflowDefinition definition,
                                      WorkflowVersion version,
                                      java.util.List<WorkflowNodeDefinition> nodeDefinitions,
                                      java.util.List<WorkflowLinkDefinition> linkDefinitions,
                                      String recordId,
                                      String operatorId,
                                      Instant operatedAt) {
        WorkflowSubmitDraft draft = submitDraftService.build(definition, version, nodeDefinitions, linkDefinitions,
                recordId, operatorId, operatedAt);
        persist(draft, operatedAt);
        return draft;
    }

    @Transactional
    public WorkflowSubmitDraft submit(WorkflowDefinition definition,
                                      WorkflowVersion version,
                                      java.util.List<WorkflowNodeDefinition> nodeDefinitions,
                                      java.util.List<WorkflowLinkDefinition> linkDefinitions,
                                      String recordId,
                                      String authOrgId,
                                      String operatorId,
                                      Instant operatedAt) {
        WorkflowSubmitDraft draft = submitDraftService.build(definition, version, nodeDefinitions, linkDefinitions,
                recordId, authOrgId, operatorId, operatedAt);
        persist(draft, operatedAt);
        return draft;
    }

    @Transactional
    public void persist(WorkflowSubmitDraft draft, Instant operatedAt) {
        Instant now = operatedAt == null ? Instant.now() : operatedAt;
        prepareInsert(draft.instance(), now);
        instanceService.beforeInsert(draft.instance());
        instanceDao.insert(draft.instance());
        draft.nodes().forEach(node -> {
            prepareInsert(node, now);
            nodeInstanceDao.insert(node);
        });
        draft.routes().forEach(route -> {
            prepareInsert(route, now);
            routeInstanceDao.insert(route);
        });
        draft.tasks().forEach(task -> {
            prepareInsert(task, now);
            taskDao.insert(task);
        });
        draft.events().forEach(event -> {
            prepareInsert(event, now);
            eventDao.insert(event);
        });
    }

    private void prepareInsert(net.ximatai.muyun.spring.common.model.contract.EntityContract entity, Instant now) {
        EntityLifecycle.prepareInsert(entity, now);
    }
}
