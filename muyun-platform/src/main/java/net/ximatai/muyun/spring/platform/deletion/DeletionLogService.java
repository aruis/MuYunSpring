package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.EntityLifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * Persistence boundary for the platform deletion lifecycle log.
 *
 * <p>This is intentionally not a deletion coordinator. Ability and domain
 * integrations will create and complete records through this service later;
 * this first version only provides a validated, append-only operation/entry
 * journal with terminal completion updates.</p>
 */
@Service
public class DeletionLogService {
    private final BaseDao<DeletionOperation, String> operationDao;
    private final BaseDao<DeletionEntry, String> entryDao;

    @Autowired
    public DeletionLogService(BaseDao<DeletionOperation, String> operationDao,
                              BaseDao<DeletionEntry, String> entryDao) {
        this.operationDao = Objects.requireNonNull(operationDao, "operationDao must not be null");
        this.entryDao = Objects.requireNonNull(entryDao, "entryDao must not be null");
    }

    @Transactional
    public String startOperation(DeletionOperation operation) {
        Objects.requireNonNull(operation, "operation must not be null");
        require(operation.getOperationType(), "operationType");
        requireText(operation.getRootModuleAlias(), "rootModuleAlias");
        requireText(operation.getRootRecordId(), "rootRecordId");
        if (operation.getStatus() == null) {
            operation.setStatus(DeletionOperationStatus.IN_PROGRESS);
        }
        if (operation.getStatus() != DeletionOperationStatus.IN_PROGRESS) {
            throw new PlatformException("Deletion operation must start in progress");
        }
        if (operation.getStartedAt() == null) {
            operation.setStartedAt(Instant.now());
        }
        if (operation.getCompletedAt() != null) {
            throw new PlatformException("Deletion operation completedAt must be null when starting");
        }
        EntityLifecycle.prepareInsert(operation, Instant.now());
        return operationDao.insert(operation);
    }

    @Transactional
    public String startEntry(DeletionEntry entry) {
        Objects.requireNonNull(entry, "entry must not be null");
        requireText(entry.getOperationId(), "operationId");
        requireOperationInProgress(entry.getOperationId());
        requireText(entry.getResourceModuleAlias(), "resourceModuleAlias");
        requireText(entry.getResourceRecordId(), "resourceRecordId");
        require(entry.getTriggerType(), "triggerType");
        if (entry.getStatus() == null) {
            entry.setStatus(DeletionEntryStatus.IN_PROGRESS);
        }
        if (entry.getStatus() != DeletionEntryStatus.IN_PROGRESS) {
            throw new PlatformException("Deletion entry must start in progress");
        }
        if (entry.getParentEntryId() != null && entryDao.findById(entry.getParentEntryId()) == null) {
            throw new PlatformException("Deletion entry parentEntryId does not exist: " + entry.getParentEntryId());
        }
        if (entry.getStartedAt() == null) {
            entry.setStartedAt(Instant.now());
        }
        if (entry.getCompletedAt() != null) {
            throw new PlatformException("Deletion entry completedAt must be null when starting");
        }
        EntityLifecycle.prepareInsert(entry, Instant.now());
        return entryDao.insert(entry);
    }

    @Transactional
    public void completeOperation(String operationId, DeletionOperationStatus status, String resultMessage) {
        requireText(operationId, "operationId");
        requireTerminal(status, "operation status");
        DeletionOperation operation = requireOperation(operationId);
        if (operation.getStatus() != DeletionOperationStatus.IN_PROGRESS) {
            throw new PlatformException("Deletion operation is already completed: " + operationId);
        }
        operation.setStatus(status);
        operation.setResultMessage(blankToNull(resultMessage));
        operation.setCompletedAt(Instant.now());
        EntityLifecycle.prepareUpdate(operation, Instant.now());
        operationDao.updateById(operation);
    }

    @Transactional
    public void completeEntry(String entryId, DeletionEntryStatus status, String resultMessage) {
        requireText(entryId, "entryId");
        requireTerminal(status, "entry status");
        DeletionEntry entry = entryDao.findById(entryId);
        if (entry == null) {
            throw new PlatformException("Deletion entry does not exist: " + entryId);
        }
        if (entry.getStatus() != DeletionEntryStatus.IN_PROGRESS) {
            throw new PlatformException("Deletion entry is already completed: " + entryId);
        }
        entry.setStatus(status);
        entry.setResultMessage(blankToNull(resultMessage));
        entry.setCompletedAt(Instant.now());
        EntityLifecycle.prepareUpdate(entry, Instant.now());
        entryDao.updateById(entry);
    }

    public DeletionOperation operation(String operationId) {
        return requireOperation(operationId);
    }

    public DeletionEntry entry(String entryId) {
        requireText(entryId, "entryId");
        DeletionEntry entry = entryDao.findById(entryId);
        if (entry == null) {
            throw new PlatformException("Deletion entry does not exist: " + entryId);
        }
        return entry;
    }

    public Criteria operationEntriesCriteria(String operationId) {
        return Criteria.of().eq("operationId", requireText(operationId, "operationId"));
    }

    private void requireOperationInProgress(String operationId) {
        if (requireOperation(operationId).getStatus() != DeletionOperationStatus.IN_PROGRESS) {
            throw new PlatformException("Deletion operation is already completed: " + operationId);
        }
    }

    private DeletionOperation requireOperation(String operationId) {
        requireText(operationId, "operationId");
        DeletionOperation operation = operationDao.findById(operationId);
        if (operation == null) {
            throw new PlatformException("Deletion operation does not exist: " + operationId);
        }
        return operation;
    }

    private void requireTerminal(DeletionOperationStatus status, String fieldName) {
        require(status, fieldName);
        if (status == DeletionOperationStatus.IN_PROGRESS) {
            throw new PlatformException("Deletion " + fieldName + " must be terminal");
        }
    }

    private void requireTerminal(DeletionEntryStatus status, String fieldName) {
        require(status, fieldName);
        if (status == DeletionEntryStatus.IN_PROGRESS) {
            throw new PlatformException("Deletion " + fieldName + " must be terminal");
        }
    }

    private void require(Object value, String fieldName) {
        if (value == null) {
            throw new PlatformException("Deletion " + fieldName + " must not be null");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new PlatformException("Deletion " + fieldName + " must not be blank");
        }
        return value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
