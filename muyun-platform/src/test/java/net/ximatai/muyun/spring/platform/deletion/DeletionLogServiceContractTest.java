package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeletionLogServiceContractTest {
    private final TestMemoryDao<DeletionOperation> operationDao = new TestMemoryDao<>();
    private final TestMemoryDao<DeletionEntry> entryDao = new TestMemoryDao<>();
    private final DeletionLogService service = new DeletionLogService(operationDao, entryDao);

    @Test
    void shouldRecordAndCompleteDeleteOperationWithCascadeEntries() {
        DeletionOperation operation = deleteOperation();

        String operationId = service.startOperation(operation);
        String rootEntryId = service.startEntry(entry(operationId, null, DeletionEntryTrigger.DIRECT, "tenant-1"));
        String childEntryId = service.startEntry(entry(operationId, rootEntryId, DeletionEntryTrigger.CASCADE, "app-1"));
        service.completeEntry(rootEntryId, DeletionEntryStatus.SUCCEEDED, null);
        service.completeEntry(childEntryId, DeletionEntryStatus.SUCCEEDED, "Tenant application removed");
        service.completeOperation(operationId, DeletionOperationStatus.SUCCEEDED, "2 resources deleted");

        assertThat(operationId).isNotBlank();
        DeletionOperation persistedOperation = service.operation(operationId);
        assertThat(persistedOperation.getStatus()).isEqualTo(DeletionOperationStatus.SUCCEEDED);
        assertThat(persistedOperation.getCompletedAt()).isNotNull();
        assertThat(persistedOperation.getResultMessage()).isEqualTo("2 resources deleted");
        assertThat(service.entry(childEntryId))
                .extracting(DeletionEntry::getParentEntryId, DeletionEntry::getTriggerType,
                        DeletionEntry::getDeleteMode, DeletionEntry::getStatus)
                .containsExactly(rootEntryId, DeletionEntryTrigger.CASCADE, DeletionEntryMode.SOFT,
                        DeletionEntryStatus.SUCCEEDED);
    }

    @Test
    void shouldRejectEntryAfterOperationCompleted() {
        String operationId = service.startOperation(deleteOperation());
        service.completeOperation(operationId, DeletionOperationStatus.SUCCEEDED, null);

        assertThatThrownBy(() -> service.startEntry(entry(operationId, null, DeletionEntryTrigger.DIRECT, "tenant-1")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    void shouldRejectNonTerminalCompletionAndUnknownParent() {
        String operationId = service.startOperation(deleteOperation());

        assertThatThrownBy(() -> service.completeOperation(operationId, DeletionOperationStatus.IN_PROGRESS, null))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("must be terminal");
        assertThatThrownBy(() -> service.startEntry(entry(operationId, "missing-entry", DeletionEntryTrigger.CASCADE, "app-1")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("parentEntryId does not exist");
    }

    @Test
    void shouldRejectParentEntryFromAnotherOperation() {
        String firstOperationId = service.startOperation(deleteOperation());
        String parentEntryId = service.startEntry(entry(firstOperationId, null, DeletionEntryTrigger.DIRECT, "tenant-1"));
        String secondOperationId = service.startOperation(deleteOperation());

        assertThatThrownBy(() -> service.startEntry(entry(secondOperationId, parentEntryId,
                DeletionEntryTrigger.CASCADE, "app-1")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("belongs to another operation");
    }

    @Test
    void shouldReadOnlyEntriesBelongingToOneOperation() {
        String firstOperationId = service.startOperation(deleteOperation());
        String secondOperationId = service.startOperation(deleteOperation());
        String firstEntryId = service.startEntry(entry(firstOperationId, null, DeletionEntryTrigger.DIRECT, "tenant-1"));
        service.startEntry(entry(secondOperationId, null, DeletionEntryTrigger.DIRECT, "tenant-2"));

        assertThat(service.operationEntries(firstOperationId))
                .extracting(DeletionEntry::getId)
                .containsExactly(firstEntryId);
    }

    @Test
    void shouldLoadLatestLifecycleForOneRecordPage() {
        DeletionOperation first = deleteOperation();
        first.setRootRecordId("tenant-1");
        String firstOperationId = service.startOperation(first);
        String firstEntryId = service.startEntry(entry(firstOperationId, null,
                DeletionEntryTrigger.DIRECT, "tenant-1"));
        service.completeEntry(firstEntryId, DeletionEntryStatus.SUCCEEDED, null);
        service.completeOperation(firstOperationId, DeletionOperationStatus.SUCCEEDED, null);

        DeletionOperation second = deleteOperation();
        second.setRootRecordId("tenant-2");
        String secondOperationId = service.startOperation(second);
        String secondEntryId = service.startEntry(entry(secondOperationId, null,
                DeletionEntryTrigger.DIRECT, "tenant-2"));
        service.completeEntry(secondEntryId, DeletionEntryStatus.SUCCEEDED, null);
        service.completeOperation(secondOperationId, DeletionOperationStatus.SUCCEEDED, null);

        Map<String, DeletionLifecycleEntry> lifecycles = service.latestTerminalEntries(
                "iam.tenant", "tenant", List.of("tenant-1", "tenant-2"));

        assertThat(lifecycles).containsOnlyKeys("tenant-1", "tenant-2");
        assertThat(lifecycles.get("tenant-1").operation().getId()).isEqualTo(firstOperationId);
        assertThat(lifecycles.get("tenant-2").operation().getId()).isEqualTo(secondOperationId);
    }

    private DeletionOperation deleteOperation() {
        DeletionOperation operation = new DeletionOperation();
        operation.setTenantId("tenant-1");
        operation.setOperationType(DeletionOperationType.DELETE);
        operation.setRootModuleAlias("iam.tenant");
        operation.setRootEntityAlias("tenant");
        operation.setRootRecordId("tenant-1");
        operation.setOperatorId("admin");
        return operation;
    }

    private DeletionEntry entry(String operationId, String parentEntryId, DeletionEntryTrigger triggerType, String recordId) {
        DeletionEntry entry = new DeletionEntry();
        entry.setTenantId("tenant-1");
        entry.setOperationId(operationId);
        entry.setParentEntryId(parentEntryId);
        entry.setResourceModuleAlias(recordId.startsWith("app") ? "iam.tenant_application" : "iam.tenant");
        entry.setResourceEntityAlias(recordId.startsWith("app") ? "tenantApplication" : "tenant");
        entry.setResourceRecordId(recordId);
        entry.setTriggerType(triggerType);
        entry.setDeleteMode(DeletionEntryMode.SOFT);
        return entry;
    }
}
