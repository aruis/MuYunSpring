package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WorkflowSubmitReadFacade {
    private static final PageRequest ONE = new PageRequest(0, 1);

    private final WorkflowInstanceDao instanceDao;
    private final WorkflowDefinitionSelector selector;
    private final WorkflowSubmitFacade submitFacade;
    private final List<WorkflowModuleRecordGuard> recordGuards;

    public WorkflowSubmitReadFacade(WorkflowInstanceDao instanceDao,
                                    WorkflowDefinitionSelector selector,
                                    WorkflowSubmitFacade submitFacade) {
        this(instanceDao, selector, submitFacade, List.of());
    }

    @Autowired
    public WorkflowSubmitReadFacade(WorkflowInstanceDao instanceDao,
                                    WorkflowDefinitionSelector selector,
                                    WorkflowSubmitFacade submitFacade,
                                    List<WorkflowModuleRecordGuard> recordGuards) {
        this.instanceDao = instanceDao;
        this.selector = selector;
        this.submitFacade = submitFacade;
        this.recordGuards = recordGuards == null ? List.of() : List.copyOf(recordGuards);
    }

    public WorkflowSubmitStatusView status(WorkflowSubmitRequest request) {
        WorkflowSubmitRequest normalized = normalizeApprovalRequest(request);
        recordGuards.forEach(guard -> guard.beforeSubmit(normalized));
        WorkflowInstance current = currentInstance(normalized.moduleAlias(), normalized.recordId());
        if (current != null) {
            return WorkflowSubmitStatusView.current(current);
        }
        try {
            return WorkflowSubmitStatusView.unsubmitted(normalized.moduleAlias(), normalized.recordId(),
                    selector.select(normalized));
        } catch (PlatformException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().startsWith("published workflow definition not found:")) {
                return WorkflowSubmitStatusView.noWorkflow(normalized.moduleAlias(), normalized.recordId(),
                        exception.getMessage());
            }
            return WorkflowSubmitStatusView.matchError(normalized.moduleAlias(), normalized.recordId(),
                    exception.getMessage());
        }
    }

    public WorkflowSubmitPreviewView preview(WorkflowSubmitRequest request) {
        return WorkflowSubmitPreviewView.of(submitFacade.preview(normalizeApprovalRequest(request)));
    }

    private WorkflowInstance currentInstance(String moduleAlias, String recordId) {
        return instanceDao.query(Criteria.of()
                        .eq("moduleAlias", moduleAlias)
                        .eq("recordId", recordId),
                ONE, Sort.desc("startedAt"), Sort.desc("createdAt")).stream().findFirst().orElse(null);
    }

    private WorkflowSubmitRequest normalizeApprovalRequest(WorkflowSubmitRequest request) {
        if (request == null) {
            throw new PlatformException("workflow submit request must not be null");
        }
        return WorkflowSubmitRequest.approval(request.moduleAlias(), request.recordId())
                .withAuthOrgId(request.authOrgId())
                .withOperator(request.operatorId())
                .withOperatedAt(request.operatedAt())
                .withSelectedRoute(request.selectedRouteKey(), request.selectedReason())
                .withManualRouteSelections(request.manualRouteSelections());
    }
}
