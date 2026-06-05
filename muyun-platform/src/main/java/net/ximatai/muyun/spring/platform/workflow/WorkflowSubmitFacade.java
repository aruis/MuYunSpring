package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class WorkflowSubmitFacade {
    private final WorkflowDefinitionSelector selector;
    private final WorkflowRuntimeSubmitService runtimeSubmitService;
    private final Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter;
    private final List<WorkflowModuleRecordGuard> recordGuards;

    public WorkflowSubmitFacade(WorkflowDefinitionSelector selector,
                                WorkflowRuntimeSubmitService runtimeSubmitService,
                                Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter) {
        this(selector, runtimeSubmitService, approvalSummaryWriter, List.of());
    }

    @Autowired
    public WorkflowSubmitFacade(WorkflowDefinitionSelector selector,
                                WorkflowRuntimeSubmitService runtimeSubmitService,
                                Optional<WorkflowApprovalSummaryWriter> approvalSummaryWriter,
                                List<WorkflowModuleRecordGuard> recordGuards) {
        this.selector = selector;
        this.runtimeSubmitService = runtimeSubmitService;
        this.approvalSummaryWriter = approvalSummaryWriter == null ? Optional.empty() : approvalSummaryWriter;
        this.recordGuards = recordGuards == null ? List.of() : List.copyOf(recordGuards);
    }

    @Transactional
    public WorkflowSubmitResult submit(WorkflowSubmitRequest request) {
        WorkflowSubmitRequest normalized = normalize(request);
        recordGuards.forEach(guard -> guard.beforeSubmit(normalized));
        WorkflowDefinitionSelection selection = selector.select(normalized);
        WorkflowSubmitDraft draft = normalized.authOrgId() == null
                ? runtimeSubmitService.submit(
                selection.definition(),
                selection.version(),
                selection.nodes(),
                selection.links(),
                normalized.recordId(),
                normalized.operatorId(),
                normalized.operatedAt())
                : runtimeSubmitService.submit(
                selection.definition(),
                selection.version(),
                selection.nodes(),
                selection.links(),
                normalized.recordId(),
                normalized.authOrgId(),
                normalized.operatorId(),
                normalized.operatedAt());
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
        String moduleAlias = requireText(request.moduleAlias(), "moduleAlias");
        String recordId = requireText(request.recordId(), "recordId");
        String operatorId = request.operatorId();
        if (operatorId == null || operatorId.isBlank()) {
            operatorId = CurrentUserContext.currentUser()
                    .map(user -> user.userId())
                    .orElse("system");
        }
        Instant operatedAt = request.operatedAt() == null ? Instant.now() : request.operatedAt();
        String authOrgId = request.authOrgId();
        if (authOrgId != null && authOrgId.isBlank()) {
            authOrgId = null;
        }
        return new WorkflowSubmitRequest(moduleAlias, recordId, request.approvalRequired(),
                request.definitionAlias(), authOrgId, operatorId, operatedAt);
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(name + " must not be blank");
        }
        return value;
    }
}
