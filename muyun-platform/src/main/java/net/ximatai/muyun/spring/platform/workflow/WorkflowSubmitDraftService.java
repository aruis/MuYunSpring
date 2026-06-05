package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class WorkflowSubmitDraftService {
    private final WorkflowInstanceSnapshotFactory snapshotFactory;
    private final WorkflowRuntimeActivationService activationService;
    private final WorkflowInstanceStateService instanceStateService;
    private final WorkflowNodeInstanceStateService nodeInstanceStateService;
    private final WorkflowRouteInstanceStateService routeInstanceStateService;
    private final WorkflowRuntimeTaskFactory taskFactory;

    public WorkflowSubmitDraftService(WorkflowInstanceSnapshotFactory snapshotFactory,
                                      WorkflowRuntimeActivationService activationService,
                                      WorkflowInstanceStateService instanceStateService,
                                      WorkflowNodeInstanceStateService nodeInstanceStateService,
                                      WorkflowRouteInstanceStateService routeInstanceStateService,
                                      WorkflowRuntimeTaskFactory taskFactory) {
        this.snapshotFactory = snapshotFactory;
        this.activationService = activationService;
        this.instanceStateService = instanceStateService;
        this.nodeInstanceStateService = nodeInstanceStateService;
        this.routeInstanceStateService = routeInstanceStateService;
        this.taskFactory = taskFactory;
    }

    public WorkflowSubmitDraft build(WorkflowDefinition definition,
                                     WorkflowVersion version,
                                     List<WorkflowNodeDefinition> nodeDefinitions,
                                     List<WorkflowLinkDefinition> linkDefinitions,
                                     String recordId,
                                     String operatorId,
                                     Instant operatedAt) {
        return build(definition, version, nodeDefinitions, linkDefinitions, recordId, null, operatorId, operatedAt);
    }

    public WorkflowSubmitDraft build(WorkflowDefinition definition,
                                     WorkflowVersion version,
                                     List<WorkflowNodeDefinition> nodeDefinitions,
                                     List<WorkflowLinkDefinition> linkDefinitions,
                                     String recordId,
                                     String authOrgId,
                                     String operatorId,
                                     Instant operatedAt) {
        WorkflowInstanceSnapshot snapshot = snapshotFactory.build(definition, version, nodeDefinitions,
                linkDefinitions, recordId, authOrgId, operatorId, operatedAt);
        WorkflowRuntimeGraph graph = WorkflowRuntimeGraph.of(nodeDefinitions, linkDefinitions);
        WorkflowNodeDefinition startNode = graph.startNodes().stream()
                .findFirst()
                .orElseThrow(() -> new PlatformException("workflow must contain a start node"));
        WorkflowActivationResult activation = activationService.activate(
                WorkflowActivationRequest.from(graph, startNode.getNodeKey()));
        instanceStateService.applyActivation(snapshot.instance(), activation, operatedAt);
        nodeInstanceStateService.applyActivation(snapshot.nodes(), activation, operatedAt);
        routeInstanceStateService.applyActivation(snapshot.routes(), activation, operatorId, operatedAt);
        WorkflowRuntimeTaskDraft taskDraft = taskFactory.createBlockingTasks(snapshot.instance(), snapshot.nodes(),
                activation, operatorId, operatedAt);

        List<WorkflowEvent> events = new ArrayList<>();
        events.addAll(snapshot.events());
        events.addAll(taskDraft.events());
        return new WorkflowSubmitDraft(snapshot.instance(), snapshot.nodes(), snapshot.routes(),
                taskDraft.tasks(), events, activation);
    }
}
