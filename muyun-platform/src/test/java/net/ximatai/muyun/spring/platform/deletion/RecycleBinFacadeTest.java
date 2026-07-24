package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecycleBinFacadeTest {
    @Test
    void shouldExposeOnlyRootSoftDeleteOperationAsRecoverable() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        SoftDeleteRestoreCoordinator coordinator = mock(SoftDeleteRestoreCoordinator.class);
        StandardEntity record = deletedRecord("tenant-1");
        when(ability.getModuleAlias()).thenReturn("iam.tenant");
        when(ability.listRecycleBin(any(PageRequest.class))).thenReturn(List.of(record));
        when(logService.latestTerminalEntry("iam.tenant", "tenant-1"))
                .thenReturn(new DeletionLifecycleEntry(successfulDelete("delete-1", "iam.tenant", "tenant-1"), successfulEntry()));

        RecycleBinItem<StandardEntity> item = new RecycleBinFacade(logService, coordinator)
                .list(ability, PageRequest.of(1, 20)).getFirst();

        assertThat(item.sourceDeleteOperationId()).isEqualTo("delete-1");
        assertThat(item.restorable()).isTrue();
    }

    @Test
    void shouldRejectRestoreFromAnotherResourceRoot() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        SoftDeleteRestoreCoordinator coordinator = mock(SoftDeleteRestoreCoordinator.class);
        when(ability.getModuleAlias()).thenReturn("iam.tenant");
        when(logService.operation("delete-1")).thenReturn(successfulDelete("delete-1", "iam.role", "role-1"));

        assertThatThrownBy(() -> new RecycleBinFacade(logService, coordinator).restore(ability, "delete-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("iam.tenant");
        verify(ability).beforeRecycleBinRestore();
    }

    @Test
    void shouldDelegateEligibleRestoreToCoordinatorAfterResourceBoundaryHook() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        SoftDeleteRestoreCoordinator coordinator = mock(SoftDeleteRestoreCoordinator.class);
        RestoreReport expected = new RestoreReport("delete-1", "restore-1", List.of());
        when(ability.getModuleAlias()).thenReturn("iam.tenant");
        when(logService.operation("delete-1")).thenReturn(successfulDelete("delete-1", "iam.tenant", "tenant-1"));
        when(coordinator.restore("delete-1")).thenReturn(expected);

        RestoreReport actual = new RecycleBinFacade(logService, coordinator).restore(ability, "delete-1");

        assertThat(actual).isSameAs(expected);
        verify(ability).beforeRecycleBinRestore();
        verify(coordinator).restore("delete-1");
    }

    private static StandardEntity deletedRecord(String id) {
        StandardEntity record = new TestRecord();
        record.setId(id);
        record.setDeleted(Boolean.TRUE);
        record.setDeletedAt(Instant.parse("2026-07-24T00:00:00Z"));
        return record;
    }

    private static DeletionOperation successfulDelete(String id, String moduleAlias, String recordId) {
        DeletionOperation operation = new DeletionOperation();
        operation.setId(id);
        operation.setOperationType(DeletionOperationType.DELETE);
        operation.setStatus(DeletionOperationStatus.SUCCEEDED);
        operation.setRootModuleAlias(moduleAlias);
        operation.setRootRecordId(recordId);
        return operation;
    }

    private static DeletionEntry successfulEntry() {
        DeletionEntry entry = new DeletionEntry();
        entry.setStatus(DeletionEntryStatus.SUCCEEDED);
        entry.setDeleteMode(DeletionEntryMode.SOFT);
        return entry;
    }

    private static final class TestRecord extends StandardEntity {
    }
}
