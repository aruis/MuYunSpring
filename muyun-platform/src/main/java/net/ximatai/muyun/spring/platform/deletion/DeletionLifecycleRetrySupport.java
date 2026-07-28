package net.ximatai.muyun.spring.platform.deletion;

/** Shared source-chain identity checks for resumable restore and purge operations. */
final class DeletionLifecycleRetrySupport {
    private DeletionLifecycleRetrySupport() {
    }

    static boolean completedByEarlierAttempt(DeletionLifecycleEntry latest,
                                             DeletionEntry sourceEntry,
                                             DeletionOperationType operationType,
                                             String sourceOperationId) {
        if (latest == null || latest.operation() == null || latest.entry() == null) {
            return false;
        }
        DeletionOperation operation = latest.operation();
        DeletionEntry entry = latest.entry();
        return operation.getOperationType() == operationType
                && sourceOperationId.equals(operation.getSourceOperationId())
                && sourceEntry.getId().equals(entry.getSourceEntryId())
                && entry.getStatus() == DeletionEntryStatus.SUCCEEDED;
    }
}
