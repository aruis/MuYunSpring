package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.ability.CrudAbility;
import net.ximatai.muyun.spring.ability.deletion.DeletionContext;
import net.ximatai.muyun.spring.ability.deletion.DeletionLifecycleListener;
import net.ximatai.muyun.spring.ability.deletion.DeletionMode;
import net.ximatai.muyun.spring.ability.deletion.DeletionNode;
import net.ximatai.muyun.spring.ability.deletion.DeletionResource;
import net.ximatai.muyun.spring.ability.deletion.DeletionTrigger;
import net.ximatai.muyun.spring.common.id.Ids;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Converts the shared Ability deletion callbacks into one operation journal.
 *
 * <p>Entries are accumulated until the direct root is terminal. This lets a
 * parent entry allocate a stable ID before it recursively invokes children,
 * while avoiding partially persisted trees during ordinary cascades.</p>
 */
@Component
public class DeletionLogLifecycleListener implements DeletionLifecycleListener {
    private final DeletionLogService logService;
    private final ThreadLocal<Map<String, Journal>> journals = ThreadLocal.withInitial(LinkedHashMap::new);

    public DeletionLogLifecycleListener(DeletionLogService logService) {
        this.logService = Objects.requireNonNull(logService, "logService must not be null");
    }

    @Override
    public DeletionNode started(CrudAbility<?> ability,
                                EntityContract entity,
                                DeletionContext context,
                                DeletionMode mode) {
        Journal journal = journal(context, entity);
        String entryId = Ids.newId();
        DeletionEntry entry = new DeletionEntry();
        entry.setId(entryId);
        entry.setTenantId(entity.getTenantId());
        entry.setOperationId(context.operationId());
        entry.setParentEntryId(context.parentEntryId());
        entry.setResourceModuleAlias(ability.getModuleAlias());
        entry.setResourceRecordId(entity.getId());
        entry.setTriggerType(context.trigger() == DeletionTrigger.CASCADE
                ? DeletionEntryTrigger.CASCADE : DeletionEntryTrigger.DIRECT);
        entry.setDeleteMode(mode == DeletionMode.SOFT ? DeletionEntryMode.SOFT : DeletionEntryMode.HARD);
        entry.setResourceVersion(entity.getVersion());
        entry.setStatus(DeletionEntryStatus.IN_PROGRESS);
        entry.setStartedAt(Instant.now());
        journal.entries.put(entryId, entry);
        return new DeletionNode(entryId, new DeletionResource(ability.getModuleAlias(), entity.getId()));
    }

    @Override
    public void succeeded(CrudAbility<?> ability,
                          EntityContract entity,
                          DeletionContext context,
                          DeletionNode node,
                          DeletionMode mode) {
        Journal journal = journal(context, entity);
        completeEntry(journal, node.entryId(), DeletionEntryStatus.SUCCEEDED, null);
        if (context.trigger() == DeletionTrigger.DIRECT) {
            flush(context.operationId(), DeletionOperationStatus.SUCCEEDED, null);
        }
    }

    @Override
    public void failed(CrudAbility<?> ability,
                       EntityContract entity,
                       DeletionContext context,
                       DeletionNode node,
                       DeletionMode mode,
                       RuntimeException failure) {
        Journal journal = journal(context, entity);
        completeEntry(journal, node.entryId(), DeletionEntryStatus.FAILED,
                failure == null ? null : failure.getMessage());
        if (context.trigger() == DeletionTrigger.DIRECT) {
            flush(context.operationId(), DeletionOperationStatus.FAILED,
                    failure == null ? null : failure.getMessage());
        }
    }

    private Journal journal(DeletionContext context, EntityContract entity) {
        return journals.get().computeIfAbsent(context.operationId(), ignored -> {
            DeletionOperation operation = new DeletionOperation();
            operation.setId(context.operationId());
            operation.setTenantId(entity.getTenantId());
            operation.setOperationType(DeletionOperationType.DELETE);
            operation.setStatus(DeletionOperationStatus.IN_PROGRESS);
            operation.setRootModuleAlias(context.root().moduleAlias());
            operation.setRootRecordId(context.root().recordId());
            operation.setOperatorId(CurrentUserContext.currentUser().map(user -> user.userId()).orElse(null));
            operation.setStartedAt(Instant.now());
            return new Journal(operation);
        });
    }

    private void completeEntry(Journal journal, String entryId, DeletionEntryStatus status, String message) {
        DeletionEntry entry = journal.entries.get(entryId);
        if (entry == null || entry.getStatus() != DeletionEntryStatus.IN_PROGRESS) {
            return;
        }
        entry.setStatus(status);
        entry.setResultMessage(blankToNull(message));
        entry.setCompletedAt(Instant.now());
    }

    private void flush(String operationId, DeletionOperationStatus status, String message) {
        Journal journal = journals.get().remove(operationId);
        if (journal == null) {
            return;
        }
        try {
            logService.startOperation(journal.operation);
            for (DeletionEntry entry : journal.entries.values()) {
                DeletionEntryStatus terminalStatus = entry.getStatus();
                String terminalMessage = entry.getResultMessage();
                entry.setStatus(DeletionEntryStatus.IN_PROGRESS);
                entry.setResultMessage(null);
                entry.setCompletedAt(null);
                logService.startEntry(entry);
                if (terminalStatus == DeletionEntryStatus.IN_PROGRESS) {
                    terminalStatus = DeletionEntryStatus.SKIPPED;
                    terminalMessage = "Deletion operation ended before this resource completed";
                }
                logService.completeEntry(entry.getId(), terminalStatus, terminalMessage);
            }
            logService.completeOperation(operationId, status, blankToNull(message));
        } finally {
            if (journals.get().isEmpty()) {
                journals.remove();
            }
        }
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }

    private static final class Journal {
        private final DeletionOperation operation;
        private final Map<String, DeletionEntry> entries = new LinkedHashMap<>();

        private Journal(DeletionOperation operation) {
            this.operation = operation;
        }
    }
}
