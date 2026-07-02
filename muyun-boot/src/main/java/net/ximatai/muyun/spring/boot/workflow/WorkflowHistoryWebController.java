package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEvent;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryEventView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryQueryService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryTaskView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;

@ApplicationScoped
@Path("/workflow/history")
public class WorkflowHistoryWebController {
    private final WorkflowHistoryQueryService historyQueryService;

    public WorkflowHistoryWebController(WorkflowHistoryQueryService historyQueryService) {
        this.historyQueryService = historyQueryService;
    }

    @POST
    @Path("/query")
    public WebListResponse<WorkflowHistoryInstance> query(WorkflowHistoryQueryWebRequest request) {
        WorkflowHistoryQueryWebRequest payload = request == null
                ? new WorkflowHistoryQueryWebRequest(null, null, null, null)
                : request;
        return new WebListResponse<>(historyQueryService.queryRecordHistory(
                payload.moduleAlias(), payload.recordId(), payload.startedBy(), page(payload.page())));
    }

    @GET
    @Path("/{historyInstanceId}/bundle")
    public WorkflowRuntimeRenderBundle renderBundle(@PathParam("historyInstanceId") String historyInstanceId) {
        return historyQueryService.renderBundle(historyInstanceId);
    }

    @GET
    @Path("/{historyInstanceId}/tasks")
    public WebListResponse<WorkflowTask> tasks(@PathParam("historyInstanceId") String historyInstanceId) {
        return new WebListResponse<>(historyQueryService.tasks(historyInstanceId));
    }

    @GET
    @Path("/{historyInstanceId}/tasks/view")
    public WebListResponse<WorkflowHistoryTaskView> taskViews(@PathParam("historyInstanceId") String historyInstanceId) {
        return new WebListResponse<>(historyQueryService.taskViews(historyInstanceId));
    }

    @GET
    @Path("/{historyInstanceId}/events")
    public WebListResponse<WorkflowEvent> events(@PathParam("historyInstanceId") String historyInstanceId) {
        return new WebListResponse<>(historyQueryService.events(historyInstanceId));
    }

    @GET
    @Path("/{historyInstanceId}/events/view")
    public WebListResponse<WorkflowHistoryEventView> eventViews(@PathParam("historyInstanceId") String historyInstanceId) {
        return new WebListResponse<>(historyQueryService.eventViews(historyInstanceId));
    }

    private PageRequest page(WebPageRequest request) {
        WebPageRequest normalized = request == null ? WebPageRequest.DEFAULT : request;
        return PageRequest.of(normalized.pageNum(), normalized.pageSize());
    }

    public record WorkflowHistoryQueryWebRequest(
            String moduleAlias,
            String recordId,
            String startedBy,
            WebPageRequest page
    ) {
    }
}
