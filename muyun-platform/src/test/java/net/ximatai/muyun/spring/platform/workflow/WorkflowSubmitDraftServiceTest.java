package net.ximatai.muyun.spring.platform.workflow;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowSubmitDraftServiceTest {
    private final WorkflowRuntimeEventFactory eventFactory = new WorkflowRuntimeEventFactory();
    private final WorkflowInstanceStateService instanceStateService = new WorkflowInstanceStateService();
    private final WorkflowSubmitDraftService service = new WorkflowSubmitDraftService(
            new WorkflowInstanceSnapshotFactory(instanceStateService, eventFactory),
            new WorkflowRuntimeActivationService(),
            instanceStateService,
            new WorkflowNodeInstanceStateService(),
            new WorkflowRouteInstanceStateService(),
            new WorkflowRouteRuntimeService(),
            new WorkflowRuntimeTaskFactory(eventFactory)
    );

    @Test
    void shouldBuildSubmitDraftUntilFirstApprovalBlock() {
        WorkflowSubmitDraft draft = service.build(definition(true), version(),
                List.of(node("start", WorkflowNodeType.START), node("approve", WorkflowNodeType.APPROVAL)),
                List.of(link("r1", "start", "approve")),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z"));

        assertThat(draft.instance().getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.RUNNING);
        assertThat(draft.instance().getCurrentNodeKeys()).isEqualTo("approve");
        assertThat(draft.nodes()).hasSize(2);
        assertThat(draft.nodes()).extracting(WorkflowNodeInstance::getNodeStatus)
                .containsExactly(WorkflowNodeStatus.COMPLETED, WorkflowNodeStatus.ACTIVE);
        assertThat(draft.routes()).hasSize(1);
        assertThat(draft.routes().get(0).getRouteStatus()).isEqualTo(WorkflowRouteStatus.EFFECTIVE);
        assertThat(draft.tasks()).hasSize(1)
                .first()
                .satisfies(task -> assertThat(task.getTaskKind()).isEqualTo(WorkflowTaskKind.APPROVAL));
        assertThat(draft.events()).extracting(WorkflowEvent::getEventType)
                .containsExactly(WorkflowEventType.INSTANCE_STARTED, WorkflowEventType.TASK_CREATED);
    }

    @Test
    void shouldMarkOnlyTraversedBranchRouteEffective() {
        WorkflowSubmitDraft draft = service.build(definition(false), version(),
                List.of(node("start", WorkflowNodeType.START),
                        node("branch", WorkflowNodeType.BRANCH),
                        node("leftTask", WorkflowNodeType.TASK),
                        node("rightTask", WorkflowNodeType.TASK)),
                List.of(link("toBranch", "start", "branch"),
                        link("leftRoute", "branch", "leftTask"),
                        defaultLink("rightRoute", "branch", "rightTask")),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z"));

        assertThat(draft.activation().traversedRouteKeys()).containsExactly("toBranch", "rightRoute");
        assertThat(draft.routes()).filteredOn(route -> route.getRouteKey().equals("toBranch"))
                .first()
                .satisfies(route -> assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.EFFECTIVE));
        assertThat(draft.routes()).filteredOn(route -> route.getRouteKey().equals("rightRoute"))
                .first()
                .satisfies(route -> assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.EFFECTIVE));
        assertThat(draft.routes()).filteredOn(route -> route.getRouteKey().equals("leftRoute"))
                .first()
                .satisfies(route -> assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.CANDIDATE));
    }

    @Test
    void shouldUseSelectedRouteKeyForReachableInitialBranch() {
        WorkflowSubmitDraft draft = service.build(definition(false), version(),
                List.of(node("start", WorkflowNodeType.START),
                        node("branch", WorkflowNodeType.BRANCH),
                        node("leftTask", WorkflowNodeType.TASK),
                        node("rightTask", WorkflowNodeType.TASK)),
                List.of(link("toBranch", "start", "branch"),
                        link("leftRoute", "branch", "leftTask"),
                        defaultLink("rightRoute", "branch", "rightTask")),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z"),
                "leftRoute", "choose left");

        assertThat(draft.activation().traversedRouteKeys()).containsExactly("toBranch", "leftRoute");
        assertThat(draft.routes()).filteredOn(route -> route.getRouteKey().equals("leftRoute"))
                .first()
                .satisfies(route -> {
                    assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.EFFECTIVE);
                    assertThat(route.getRouteReason()).isEqualTo(WorkflowRouteReason.MANUAL_SELECTED);
                    assertThat(route.getSelectedBy()).isEqualTo("user-1");
                });
        assertThat(draft.routes()).filteredOn(route -> route.getRouteKey().equals("rightRoute"))
                .first()
                .satisfies(route -> {
                    assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.INEFFECTIVE);
                    assertThat(route.getRouteReason()).isEqualTo(WorkflowRouteReason.MANUAL_UNSELECTED);
                });
    }

    @Test
    void shouldRejectSubmitWhenInitialManualBranchHasNoSelectedRoute() {
        WorkflowNodeDefinition branch = manualBranch("branch", "START", false);

        assertThatThrownBy(() -> service.build(definition(false), version(),
                List.of(node("start", WorkflowNodeType.START),
                        branch,
                        node("leftTask", WorkflowNodeType.TASK),
                        node("rightTask", WorkflowNodeType.TASK)),
                List.of(link("toBranch", "start", "branch"),
                        link("leftRoute", "branch", "leftTask"),
                        defaultLink("rightRoute", "branch", "rightTask")),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z")))
                .isInstanceOf(net.ximatai.muyun.spring.common.exception.PlatformException.class)
                .hasMessageContaining("manual branch requires selected route: branch");
    }

    @Test
    void shouldAllowSubmitManualBranchWhenStartSelectorMatchesStarter() {
        WorkflowNodeDefinition branch = manualBranch("branch", "START", false);

        WorkflowSubmitDraft draft = service.build(definition(false), version(),
                List.of(node("start", WorkflowNodeType.START),
                        branch,
                        node("leftTask", WorkflowNodeType.TASK),
                        node("rightTask", WorkflowNodeType.TASK)),
                List.of(link("toBranch", "start", "branch"),
                        link("leftRoute", "branch", "leftTask"),
                        defaultLink("rightRoute", "branch", "rightTask")),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z"),
                "leftRoute", null);

        assertThat(draft.routes()).filteredOn(route -> route.getRouteKey().equals("leftRoute"))
                .first()
                .satisfies(route -> {
                    assertThat(route.getRouteStatus()).isEqualTo(WorkflowRouteStatus.EFFECTIVE);
                    assertThat(route.getRouteReason()).isEqualTo(WorkflowRouteReason.MANUAL_SELECTED);
                    assertThat(route.getSelectedBy()).isEqualTo("user-1");
                });
    }

    @Test
    void shouldRejectSubmitManualBranchWhenRequiredReasonIsMissing() {
        WorkflowNodeDefinition branch = manualBranch("branch", "START", true);

        assertThatThrownBy(() -> service.build(definition(false), version(),
                List.of(node("start", WorkflowNodeType.START),
                        branch,
                        node("leftTask", WorkflowNodeType.TASK)),
                List.of(link("toBranch", "start", "branch"),
                        link("leftRoute", "branch", "leftTask")),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z"),
                "leftRoute", null))
                .isInstanceOf(net.ximatai.muyun.spring.common.exception.PlatformException.class)
                .hasMessageContaining("selection reason is required: branch");
    }

    @Test
    void shouldRejectSelectedRouteKeyWhenSubmitActivationCannotReachBranch() {
        assertThatThrownBy(() -> service.build(definition(false), version(),
                List.of(node("start", WorkflowNodeType.START),
                        node("approve", WorkflowNodeType.APPROVAL),
                        node("branch", WorkflowNodeType.BRANCH),
                        node("leftTask", WorkflowNodeType.TASK)),
                List.of(link("toApprove", "start", "approve"),
                        link("toBranch", "approve", "branch"),
                        link("leftRoute", "branch", "leftTask")),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z"),
                "leftRoute", null))
                .isInstanceOf(net.ximatai.muyun.spring.common.exception.PlatformException.class)
                .hasMessageContaining("selected route is not candidate outgoing route");
    }

    @Test
    void shouldBuildSubmitDraftForCompletedWorkflow() {
        WorkflowSubmitDraft draft = service.build(definition(false), version(),
                List.of(node("start", WorkflowNodeType.START), node("end", WorkflowNodeType.END)),
                List.of(link("r1", "start", "end")),
                "record-1", "user-1", Instant.parse("2026-06-05T01:00:00Z"));

        assertThat(draft.instance().getInstanceStatus()).isEqualTo(WorkflowInstanceStatus.COMPLETED);
        assertThat(draft.instance().getCurrentNodeKeys()).isEmpty();
        assertThat(draft.tasks()).isEmpty();
        assertThat(draft.activation().completed()).isTrue();
    }

    private WorkflowDefinition definition(boolean approvalEnabled) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setId("definition-1");
        definition.setTenantId("tenant-1");
        definition.setModuleAlias("crm.customer");
        definition.setApprovalEnabled(approvalEnabled);
        return definition;
    }

    private WorkflowVersion version() {
        WorkflowVersion version = new WorkflowVersion();
        version.setId("version-1");
        version.setVersionNo(1);
        version.setSnapshotText("{}");
        return version;
    }

    private WorkflowNodeDefinition node(String key, WorkflowNodeType type) {
        WorkflowNodeDefinition node = new WorkflowNodeDefinition();
        node.setNodeKey(key);
        node.setNodeType(type);
        return node;
    }

    private WorkflowNodeDefinition manualBranch(String key, String selectorNodeKey, boolean requireReason) {
        WorkflowNodeDefinition node = node(key, WorkflowNodeType.BRANCH);
        node.setRouteMode(WorkflowRouteMode.MANUAL);
        node.setSelectorNodeKey(selectorNodeKey);
        node.setRequireManualSelectionReason(requireReason);
        return node;
    }

    private WorkflowLinkDefinition link(String key, String source, String target) {
        WorkflowLinkDefinition link = new WorkflowLinkDefinition();
        link.setRouteKey(key);
        link.setSourceNodeKey(source);
        link.setTargetNodeKey(target);
        return link;
    }

    private WorkflowLinkDefinition defaultLink(String key, String source, String target) {
        WorkflowLinkDefinition link = link(key, source, target);
        link.setDefaultRoute(Boolean.TRUE);
        return link;
    }
}
