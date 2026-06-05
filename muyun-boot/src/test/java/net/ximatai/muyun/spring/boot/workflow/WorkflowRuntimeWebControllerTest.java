package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.boot.web.CurrentUserWebFilter;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowApprovalStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowInstanceStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskCompletionPolicy;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskContext;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskContinueResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskEvaluation;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskProcessBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowModuleTaskRuntimeService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowNodeInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRejectResubmitMode;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeReadFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionFacade;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskActionResult;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskAvailableAction;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskDefinition;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskKind;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTaskStatus;
import net.ximatai.muyun.spring.platform.workflow.WorkflowWorkbenchCard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkflowRuntimeWebControllerTest {
    private WorkflowRuntimeReadFacade runtimeReadFacade;
    private WorkflowTaskActionFacade taskActionFacade;
    private WorkflowInstanceActionFacade instanceActionFacade;
    private WorkflowModuleTaskRuntimeService moduleTaskRuntimeService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        runtimeReadFacade = mock(WorkflowRuntimeReadFacade.class);
        taskActionFacade = mock(WorkflowTaskActionFacade.class);
        instanceActionFacade = mock(WorkflowInstanceActionFacade.class);
        moduleTaskRuntimeService = mock(WorkflowModuleTaskRuntimeService.class);
        mvc = MockMvcBuilders
                .standaloneSetup(new WorkflowRuntimeWebController(runtimeReadFacade, taskActionFacade,
                        instanceActionFacade, moduleTaskRuntimeService))
                .addFilters(new CurrentUserWebFilter(() -> Optional.of(
                        CurrentUser.tenantUser("user-1", "User", "tenant-a"))))
                .build();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        TenantContext.clear();
    }

    @Test
    void shouldExposeInstanceRenderBundle() throws Exception {
        WorkflowInstance instance = instance("inst-1");
        WorkflowNodeInstance node = new WorkflowNodeInstance();
        node.setId("node-1");
        node.setInstanceId("inst-1");
        node.setNodeKey("review");
        when(runtimeReadFacade.renderBundle("inst-1"))
                .thenReturn(new WorkflowRuntimeRenderBundle("RUNTIME", instance, List.of(node), List.of()));

        mvc.perform(get("/workflow/runtime/instance/inst-1/bundle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("RUNTIME"))
                .andExpect(jsonPath("$.instance.id").value("inst-1"))
                .andExpect(jsonPath("$.nodes[0].nodeKey").value("review"));
    }

    @Test
    void shouldExposeInstanceTasksEventsAndAvailableActions() throws Exception {
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        task.setTaskKind(WorkflowTaskKind.APPROVAL);
        task.setTaskStatus(WorkflowTaskStatus.TODO);
        when(runtimeReadFacade.instanceTasks("inst-1")).thenReturn(List.of(task));
        when(runtimeReadFacade.instanceEvents("inst-1")).thenReturn(List.of());
        when(runtimeReadFacade.instanceAvailableActions("inst-1", "operator-1"))
                .thenReturn(List.of(WorkflowTaskAvailableAction.of("complete", "通过")));

        mvc.perform(get("/workflow/runtime/instance/inst-1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].id").value("task-1"));
        mvc.perform(get("/workflow/runtime/instance/inst-1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray());
        mvc.perform(post("/workflow/runtime/instance/inst-1/actions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorId\":\"operator-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].actionCode").value("complete"));
    }

    @Test
    void shouldQueryWorkbenchCardsWithNormalizedPageAndCurrentUserFallback() throws Exception {
        WorkflowWorkbenchCard card = new WorkflowWorkbenchCard("TODO", "inst-1", "crm.contract", "record-1",
                WorkflowInstanceStatus.RUNNING, WorkflowApprovalStatus.PROCESSING, "task-1",
                WorkflowTaskKind.BUSINESS, WorkflowTaskStatus.TODO, "visit", "visit", List.of("user-1"),
                null, null, null, null, null, null, null, null, "user-1", Boolean.TRUE);
        ArgumentCaptor<PageRequest> pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        when(runtimeReadFacade.todoCards(eq("user-1"), pageCaptor.capture())).thenReturn(List.of(card));

        mvc.perform(post("/workflow/runtime/workbench/todo/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"page\":{\"pageNum\":2,\"pageSize\":30}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records[0].boardType").value("TODO"))
                .andExpect(jsonPath("$.records[0].taskId").value("task-1"));

        assertThat(pageCaptor.getValue().getOffset()).isEqualTo(30);
        assertThat(pageCaptor.getValue().getLimit()).isEqualTo(30);
    }

    @Test
    void shouldExposeDoneNoticeAndTrackingWorkbenchBoards() throws Exception {
        when(runtimeReadFacade.doneCards(eq("operator-1"), org.mockito.ArgumentMatchers.any(PageRequest.class)))
                .thenReturn(List.of());
        when(runtimeReadFacade.noticeCards(eq("operator-1"), org.mockito.ArgumentMatchers.any(PageRequest.class)))
                .thenReturn(List.of());
        when(runtimeReadFacade.trackingCards(eq("operator-1"), org.mockito.ArgumentMatchers.any(PageRequest.class)))
                .thenReturn(List.of());

        String request = "{\"operatorId\":\"operator-1\"}";
        mvc.perform(post("/workflow/runtime/workbench/done/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray());
        mvc.perform(post("/workflow/runtime/workbench/notice/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray());
        mvc.perform(post("/workflow/runtime/workbench/tracking/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray());
    }

    @Test
    void shouldExecuteTaskActionsThroughTaskActionFacade() throws Exception {
        WorkflowTask task = new WorkflowTask();
        task.setId("task-1");
        when(taskActionFacade.execute(eq("reject"), argThat(request ->
                "task-1".equals(request.taskId())
                        && "operator-1".equals(request.operatorId())
                        && request.rejectResubmitMode() == WorkflowRejectResubmitMode.RETURN_TO_ME
                        && "not ok".equals(request.reason()))))
                .thenReturn(WorkflowTaskActionResult.of(task, null));
        when(taskActionFacade.execute(eq("transfer"), argThat(request ->
                "task-1".equals(request.taskId())
                        && "user-1".equals(request.operatorId())
                        && "user-2".equals(request.targetAssigneeId())
                        && "handoff".equals(request.reason()))))
                .thenReturn(WorkflowTaskActionResult.transferred(task, new WorkflowTask(), null));
        when(taskActionFacade.execute(eq("addSign"), argThat(request ->
                "task-1".equals(request.taskId())
                        && "operator-1".equals(request.operatorId())
                        && request.targetAssigneeId() == null
                        && request.addSignMode() == null
                        && request.addSignSegment() != null
                        && request.addSignSegment().nodeDefinitions().size() == 1
                        && "add-1".equals(request.addSignSegment().nodeDefinitions().getFirst().getNodeKey())
                        && "user:add-signer-1".equals(request.addSignSegment().nodeDefinitions().getFirst()
                                .getParticipantPolicyText())
                        && request.addSignSegment().linkDefinitions().size() == 2
                        && "need review".equals(request.reason()))))
                .thenReturn(new WorkflowTaskActionResult(task, null, null, null, null));

        mvc.perform(post("/workflow/runtime/task/task-1/actions/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "operator-1",
                                  "rejectResubmitMode": "return_to_me",
                                  "reason": "not ok"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.task.id").value("task-1"));

        mvc.perform(post("/workflow/runtime/task/task-1/actions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetAssigneeId": "user-2",
                                  "reason": "handoff"
                                }
                                """))
                .andExpect(status().isOk());

        mvc.perform(post("/workflow/runtime/task/task-1/actions/addSign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "operatorId": "operator-1",
                                  "addSignSegment": {
                                    "nodeDefinitions": [
                                      {
                                        "nodeKey": "add-1",
                                        "nodeType": "APPROVAL",
                                        "approvalMode": "ALL",
                                        "participantPolicyText": "user:add-signer-1",
                                        "allowAddSign": true
                                      }
                                    ],
                                    "linkDefinitions": [
                                      {
                                        "routeKey": "entry-add",
                                        "sourceNodeKey": "approve",
                                        "targetNodeKey": "add-1"
                                      },
                                      {
                                        "routeKey": "exit-add",
                                        "sourceNodeKey": "add-1",
                                        "targetNodeKey": "next"
                                      }
                                    ]
                                  },
                                  "reason": "need review"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExecuteInstanceActionsThroughInstanceActionFacade() throws Exception {
        WorkflowInstance instance = instance("inst-1");
        when(instanceActionFacade.execute(eq("revoke"), argThat(request ->
                "inst-1".equals(request.instanceId())
                        && "operator-1".equals(request.operatorId())
                        && "cancel".equals(request.reason()))))
                .thenReturn(new WorkflowInstanceActionResult(instance, List.of(), List.of(), List.of(), null));
        when(instanceActionFacade.execute(eq("terminate"), argThat(request ->
                "inst-1".equals(request.instanceId())
                        && "user-1".equals(request.operatorId())
                        && "stop".equals(request.reason()))))
                .thenReturn(new WorkflowInstanceActionResult(instance, List.of(), List.of(), List.of(), null));

        mvc.perform(post("/workflow/runtime/instance/inst-1/actions/revoke")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorId\":\"operator-1\",\"reason\":\"cancel\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instance.id").value("inst-1"));

        mvc.perform(post("/workflow/runtime/instance/inst-1/actions/terminate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"stop\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldExposeModuleTaskPrepareAndCheckAndContinue() throws Exception {
        WorkflowModuleTaskProcessBundle bundle = moduleTaskBundle("task-1");
        WorkflowTaskActionResult actionResult = WorkflowTaskActionResult.of(new WorkflowTask(), null);
        when(moduleTaskRuntimeService.prepare("task-1", "user-1")).thenReturn(bundle);
        when(moduleTaskRuntimeService.checkAndContinue("task-1", "operator-1", "done"))
                .thenReturn(WorkflowModuleTaskContinueResult.continued(actionResult));

        mvc.perform(get("/workflow/runtime/task/task-1/module-task/prepare"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value("task-1"))
                .andExpect(jsonPath("$.workflowTaskContext.checkAndContinuePath")
                        .value("/workflow/runtime/task/task-1/module-task/check-and-continue"));

        mvc.perform(post("/workflow/runtime/task/task-1/module-task/check-and-continue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"operatorId\":\"operator-1\",\"reason\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.continued").value(true));

        verify(moduleTaskRuntimeService).prepare("task-1", "user-1");
        verify(moduleTaskRuntimeService).checkAndContinue("task-1", "operator-1", "done");
    }

    private WorkflowInstance instance(String id) {
        WorkflowInstance instance = new WorkflowInstance();
        instance.setId(id);
        instance.setDefinitionId("def-1");
        instance.setWorkflowVersionId("ver-1");
        instance.setVersionNo(1);
        instance.setModuleAlias("crm.contract");
        instance.setRecordId("record-1");
        instance.setInstanceStatus(WorkflowInstanceStatus.RUNNING);
        instance.setApprovalStatus(WorkflowApprovalStatus.PROCESSING);
        instance.setSnapshotText("{}");
        return instance;
    }

    private WorkflowModuleTaskProcessBundle moduleTaskBundle(String taskId) {
        WorkflowTaskDefinition definition = new WorkflowTaskDefinition();
        definition.setId("task-def-1");
        definition.setModuleAlias("crm.contract");
        definition.setAlias("visit");
        return new WorkflowModuleTaskProcessBundle(taskId, "inst-1", "visit", "crm.contract", "record-1",
                WorkflowModuleTaskCompletionPolicy.MANUAL_CONFIRM,
                new WorkflowModuleTaskContext(taskId, WorkflowModuleTaskCompletionPolicy.MANUAL_CONFIRM,
                        "/workflow/runtime/task/" + taskId + "/module-task/check-and-continue"),
                definition,
                WorkflowModuleTaskEvaluation.manualConfirm(List.of()),
                null);
    }
}
