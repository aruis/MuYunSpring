package net.ximatai.muyun.spring.boot.workflow;

import net.ximatai.muyun.spring.boot.web.WebListResponse;
import net.ximatai.muyun.spring.boot.web.WebPageRequest;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.workflow.WorkflowEvent;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryEventView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryInstance;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryQueryService;
import net.ximatai.muyun.spring.platform.workflow.WorkflowHistoryTaskView;
import net.ximatai.muyun.spring.platform.workflow.WorkflowRuntimeRenderBundle;
import net.ximatai.muyun.spring.platform.workflow.WorkflowTask;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/workflow/history")
public class WorkflowHistoryWebController {
    private final WorkflowHistoryQueryService historyQueryService;

    public WorkflowHistoryWebController(WorkflowHistoryQueryService historyQueryService) {
        this.historyQueryService = historyQueryService;
    }

    @PostMapping("/query")
    public WebListResponse<WorkflowHistoryInstance> query(@RequestBody WorkflowHistoryQueryWebRequest request) {
        WorkflowHistoryQueryWebRequest payload = request == null
                ? new WorkflowHistoryQueryWebRequest(null, null, null)
                : request;
        return new WebListResponse<>(historyQueryService.queryRecordHistory(
                payload.moduleAlias(), payload.recordId(), page(payload.page())));
    }

    @GetMapping("/{historyInstanceId}/bundle")
    public WorkflowRuntimeRenderBundle renderBundle(@PathVariable String historyInstanceId) {
        return historyQueryService.renderBundle(historyInstanceId);
    }

    @GetMapping("/{historyInstanceId}/tasks")
    public WebListResponse<WorkflowTask> tasks(@PathVariable String historyInstanceId) {
        return new WebListResponse<>(historyQueryService.tasks(historyInstanceId));
    }

    @GetMapping("/{historyInstanceId}/tasks/view")
    public WebListResponse<WorkflowHistoryTaskView> taskViews(@PathVariable String historyInstanceId) {
        return new WebListResponse<>(historyQueryService.taskViews(historyInstanceId));
    }

    @GetMapping("/{historyInstanceId}/events")
    public WebListResponse<WorkflowEvent> events(@PathVariable String historyInstanceId) {
        return new WebListResponse<>(historyQueryService.events(historyInstanceId));
    }

    @GetMapping("/{historyInstanceId}/events/view")
    public WebListResponse<WorkflowHistoryEventView> eventViews(@PathVariable String historyInstanceId) {
        return new WebListResponse<>(historyQueryService.eventViews(historyInstanceId));
    }

    @ExceptionHandler(PlatformException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public WorkflowWebError handlePlatformException(PlatformException ex) {
        return new WorkflowWebError("bad_request", ex.getMessage());
    }

    private PageRequest page(WebPageRequest request) {
        WebPageRequest normalized = request == null ? WebPageRequest.DEFAULT : request;
        return PageRequest.of(normalized.pageNum(), normalized.pageSize());
    }

    public record WorkflowHistoryQueryWebRequest(
            String moduleAlias,
            String recordId,
            WebPageRequest page
    ) {
    }
}
