package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class WorkflowSubmitFacade {
    private final WorkflowDefinitionSelector selector;
    private final WorkflowRuntimeSubmitService runtimeSubmitService;
    private final Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter;

    public WorkflowSubmitFacade(WorkflowDefinitionSelector selector,
                                WorkflowRuntimeSubmitService runtimeSubmitService,
                                Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter) {
        this.selector = selector;
        this.runtimeSubmitService = runtimeSubmitService;
        this.approvalSummaryWriter = approvalSummaryWriter == null ? Optional.empty() : approvalSummaryWriter;
    }

    @Transactional
    public WorkflowSubmitResult submit(WorkflowSubmitRequest request) {
        WorkflowSubmitRequest normalized = normalize(request);
        WorkflowDefinitionSelection selection = selector.select(normalized);
        WorkflowSubmitDraft draft = runtimeSubmitService.submit(
                selection.definition(),
                selection.version(),
                selection.nodes(),
                selection.links(),
                normalized.recordId(),
                normalized.operatorId(),
                normalized.operatedAt()
        );
        boolean written = writeApprovalSummaryIfNeeded(normalized, draft);
        return new WorkflowSubmitResult(draft, written);
    }

    private boolean writeApprovalSummaryIfNeeded(WorkflowSubmitRequest request, WorkflowSubmitDraft draft) {
        if (!draft.instance().getApprovalEnabled()) {
            return false;
        }
        WorkflowApprovalSummaryWriter writer = approvalSummaryWriter
                .orElseThrow(() -> new PlatformException("workflow approval summary writer is not configured"));
        writer.writeSubmitted(new WorkflowApprovalSummary(
                request.moduleAlias(),
                request.recordId(),
                draft.instance().getId(),
                draft.instance().getApprovalStatus(),
                request.operatorId(),
                draft.instance().getStartedAt(),
                draft.instance().getApprovalCompletedAt()
        ));
        return true;
    }

    private WorkflowSubmitRequest normalize(WorkflowSubmitRequest request) {
        if (request == null) {
            throw new PlatformException("workflow submit request must not be null");
        }
        String recordId = requireText(request.recordId(), "recordId");
        String operatorId = request.operatorId();
        if (operatorId == null || operatorId.isBlank()) {
            operatorId = CurrentUserContext.currentUser()
                    .map(user -> user.userId())
                    .orElse("system");
        }
        Instant operatedAt = request.operatedAt() == null ? Instant.now() : request.operatedAt();
        return new WorkflowSubmitRequest(request.moduleAlias(), recordId, request.approvalRequired(),
                request.definitionAlias(), operatorId, operatedAt);
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(name + " must not be blank");
        }
        return value;
    }
}
