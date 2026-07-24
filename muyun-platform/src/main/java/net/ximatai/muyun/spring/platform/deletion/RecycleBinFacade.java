package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Platform facade for the operator-facing part of an opted-in recycle bin.
 * Resource services retain ownership of visibility and authorization hooks;
 * source-tree validation and recovery execution stay in the platform.
 */
@Service
public class RecycleBinFacade {
    private final DeletionLogService deletionLogService;
    private final SoftDeleteRestoreCoordinator restoreCoordinator;

    public RecycleBinFacade(DeletionLogService deletionLogService,
                            SoftDeleteRestoreCoordinator restoreCoordinator) {
        this.deletionLogService = Objects.requireNonNull(deletionLogService, "deletionLogService must not be null");
        this.restoreCoordinator = Objects.requireNonNull(restoreCoordinator, "restoreCoordinator must not be null");
    }

    public <T extends EntityContract> List<RecycleBinItem<T>> list(RecycleBinAbility<T> ability,
                                                                      PageRequest pageRequest) {
        Objects.requireNonNull(ability, "recycleBinAbility must not be null");
        return ability.listRecycleBin(pageRequest).stream().map(record -> item(ability, record)).toList();
    }

    public RestoreReport restore(RecycleBinAbility<?> ability, String sourceDeleteOperationId) {
        Objects.requireNonNull(ability, "recycleBinAbility must not be null");
        ability.beforeRecycleBinRestore();
        DeletionOperation source = deletionLogService.operation(sourceDeleteOperationId);
        if (source.getOperationType() != DeletionOperationType.DELETE
                || source.getStatus() != DeletionOperationStatus.SUCCEEDED
                || !ability.getModuleAlias().equals(source.getRootModuleAlias())) {
            throw new PlatformException("Recycle-bin restore requires a successful root delete operation for "
                    + ability.getModuleAlias() + ": " + sourceDeleteOperationId);
        }
        return restoreCoordinator.restore(sourceDeleteOperationId);
    }

    private <T extends EntityContract> RecycleBinItem<T> item(RecycleBinAbility<T> ability, T record) {
        DeletionLifecycleEntry lifecycle = deletionLogService.latestTerminalEntry(ability.getModuleAlias(), record.getId());
        if (lifecycle == null) {
            return new RecycleBinItem<>(record, null, record.getDeletedAt(), false,
                    "deletion history is unavailable");
        }
        DeletionOperation operation = lifecycle.operation();
        DeletionEntry entry = lifecycle.entry();
        boolean restorable = operation.getOperationType() == DeletionOperationType.DELETE
                && operation.getStatus() == DeletionOperationStatus.SUCCEEDED
                && entry.getStatus() == DeletionEntryStatus.SUCCEEDED
                && entry.getDeleteMode() == DeletionEntryMode.SOFT
                && ability.getModuleAlias().equals(operation.getRootModuleAlias())
                && record.getId().equals(operation.getRootRecordId());
        return new RecycleBinItem<>(record, restorable ? operation.getId() : null, record.getDeletedAt(), restorable,
                restorable ? null : "resource lifecycle changed after deletion");
    }
}
