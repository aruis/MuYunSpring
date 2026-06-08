package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkflowRuntimeSubmitService {
    private final WorkflowSubmitDraftService submitDraftService;
    private final WorkflowInstanceService instanceService;
    private final WorkflowInstanceDao instanceDao;
    private final WorkflowNodeInstanceDao nodeInstanceDao;
    private final WorkflowRouteInstanceDao routeInstanceDao;
    private final WorkflowTaskDao taskDao;
    private final WorkflowEventDao eventDao;
    private final WorkflowRuntimePluginDispatcher pluginDispatcher;

    public WorkflowRuntimeSubmitService(WorkflowSubmitDraftService submitDraftService,
                                        WorkflowInstanceService instanceService,
                                        WorkflowInstanceDao instanceDao,
                                        WorkflowNodeInstanceDao nodeInstanceDao,
                                        WorkflowRouteInstanceDao routeInstanceDao,
                                        WorkflowTaskDao taskDao,
                                        WorkflowEventDao eventDao) {
        this(submitDraftService, instanceService, instanceDao, nodeInstanceDao, routeInstanceDao, taskDao, eventDao,
                null);
    }

    @Autowired
    public WorkflowRuntimeSubmitService(WorkflowSubmitDraftService submitDraftService,
                                        WorkflowInstanceService instanceService,
                                        WorkflowInstanceDao instanceDao,
                                        WorkflowNodeInstanceDao nodeInstanceDao,
                                        WorkflowRouteInstanceDao routeInstanceDao,
                                        WorkflowTaskDao taskDao,
                                        WorkflowEventDao eventDao,
                                        WorkflowRuntimePluginDispatcher pluginDispatcher) {
        this.submitDraftService = submitDraftService;
        this.instanceService = instanceService;
        this.instanceDao = instanceDao;
        this.nodeInstanceDao = nodeInstanceDao;
        this.routeInstanceDao = routeInstanceDao;
        this.taskDao = taskDao;
        this.eventDao = eventDao;
        this.pluginDispatcher = pluginDispatcher == null ? new WorkflowRuntimePluginDispatcher(List.of()) : pluginDispatcher;
    }

    @Transactional
    public WorkflowSubmitDraft submit(WorkflowDefinition definition,
                                      WorkflowVersion version,
                                      java.util.List<WorkflowNodeDefinition> nodeDefinitions,
                                      java.util.List<WorkflowLinkDefinition> linkDefinitions,
                                      String recordId,
                                      String operatorId,
                                      Instant operatedAt) {
        return submit(definition, version, nodeDefinitions, linkDefinitions, recordId, operatorId, operatedAt,
                null, null);
    }

    @Transactional
    public WorkflowSubmitDraft submit(WorkflowDefinition definition,
                                      WorkflowVersion version,
                                      java.util.List<WorkflowNodeDefinition> nodeDefinitions,
                                      java.util.List<WorkflowLinkDefinition> linkDefinitions,
                                      String recordId,
                                      String operatorId,
                                      Instant operatedAt,
                                      String selectedRouteKey,
                                      String selectedReason) {
        return submit(definition, version, nodeDefinitions, linkDefinitions, recordId, operatorId, operatedAt,
                selectedRouteKey, selectedReason, List.of());
    }

    @Transactional
    public WorkflowSubmitDraft submit(WorkflowDefinition definition,
                                      WorkflowVersion version,
                                      java.util.List<WorkflowNodeDefinition> nodeDefinitions,
                                      java.util.List<WorkflowLinkDefinition> linkDefinitions,
                                      String recordId,
                                      String operatorId,
                                      Instant operatedAt,
                                      String selectedRouteKey,
                                      String selectedReason,
                                      List<WorkflowManualRouteSelection> manualRouteSelections) {
        WorkflowSubmitDraft draft = submitDraftService.build(definition, version, nodeDefinitions, linkDefinitions,
                recordId, null, operatorId, operatedAt, selectedRouteKey, selectedReason, manualRouteSelections);
        dispatchSubmit(draft, WorkflowRuntimePluginEventType.BEFORE_SUBMIT, operatorId);
        persist(draft, operatedAt);
        dispatchSubmit(draft, WorkflowRuntimePluginEventType.AFTER_SUBMIT, operatorId);
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
        return submit(definition, version, nodeDefinitions, linkDefinitions, recordId, authOrgId, operatorId,
                operatedAt, null, null);
    }

    @Transactional
    public WorkflowSubmitDraft submit(WorkflowDefinition definition,
                                      WorkflowVersion version,
                                      java.util.List<WorkflowNodeDefinition> nodeDefinitions,
                                      java.util.List<WorkflowLinkDefinition> linkDefinitions,
                                      String recordId,
                                      String authOrgId,
                                      String operatorId,
                                      Instant operatedAt,
                                      String selectedRouteKey,
                                      String selectedReason) {
        return submit(definition, version, nodeDefinitions, linkDefinitions, recordId, authOrgId, operatorId,
                operatedAt, selectedRouteKey, selectedReason, List.of());
    }

    @Transactional
    public WorkflowSubmitDraft submit(WorkflowDefinition definition,
                                      WorkflowVersion version,
                                      java.util.List<WorkflowNodeDefinition> nodeDefinitions,
                                      java.util.List<WorkflowLinkDefinition> linkDefinitions,
                                      String recordId,
                                      String authOrgId,
                                      String operatorId,
                                      Instant operatedAt,
                                      String selectedRouteKey,
                                      String selectedReason,
                                      List<WorkflowManualRouteSelection> manualRouteSelections) {
        WorkflowSubmitDraft draft = submitDraftService.build(definition, version, nodeDefinitions, linkDefinitions,
                recordId, authOrgId, operatorId, operatedAt, selectedRouteKey, selectedReason,
                manualRouteSelections);
        dispatchSubmit(draft, WorkflowRuntimePluginEventType.BEFORE_SUBMIT, operatorId);
        persist(draft, operatedAt);
        dispatchSubmit(draft, WorkflowRuntimePluginEventType.AFTER_SUBMIT, operatorId);
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

    private void dispatchSubmit(WorkflowSubmitDraft draft, WorkflowRuntimePluginEventType eventType,
                                String operatorId) {
        if (draft == null || draft.instance() == null) {
            return;
        }
        Map<String, WorkflowNodeInstance> nodesById = new LinkedHashMap<>();
        draft.nodes().forEach(node -> nodesById.put(node.getId(), node));
        if (!draft.tasks().isEmpty()) {
            draft.tasks().forEach(task -> dispatch(draft.instance(), nodesById.get(task.getNodeInstanceId()), task,
                    eventType, "submit", operatorId, null, null, null, null));
            return;
        }
        if (!draft.nodes().isEmpty()) {
            draft.nodes().stream()
                    .filter(node -> node.getNodeStatus() == WorkflowNodeStatus.ACTIVE)
                    .forEach(node -> dispatch(draft.instance(), node, null, eventType, "submit", operatorId,
                            null, null, null, null));
            return;
        }
        dispatch(draft.instance(), null, null, eventType, "submit", operatorId, null, null, null, null);
    }

    private void dispatch(WorkflowInstance instance,
                          WorkflowNodeInstance node,
                          WorkflowTask task,
                          WorkflowRuntimePluginEventType eventType,
                          String actionCode,
                          String operatorId,
                          String targetAssigneeId,
                          String rollbackTargetNodeKey,
                          WorkflowRuntimeTerminateMode terminateMode,
                          String reason) {
        pluginDispatcher.dispatch(new WorkflowRuntimePluginContext(eventType, actionCode,
                instance.getModuleAlias(), instance.getRecordId(), instance.getId(),
                node == null ? null : node.getNodeKey(), task == null ? null : task.getId(),
                operatorId, targetAssigneeId, rollbackTargetNodeKey, terminateMode, reason, instance, node, task));
    }
}
