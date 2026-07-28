package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.RecycleBinAbility;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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
        RecycleBinPurgeCoordinator purgeCoordinator = mock(RecycleBinPurgeCoordinator.class);
        StandardEntity record = deletedRecord("tenant-1");
        when(ability.getModuleAlias()).thenReturn("iam.tenant");
        when(ability.getDeletionEntityAlias()).thenReturn("tenant");
        when(ability.listRecycleBin(any(PageRequest.class))).thenReturn(List.of(record));
        when(logService.latestTerminalEntry("iam.tenant", "tenant", "tenant-1"))
                .thenReturn(new DeletionLifecycleEntry(successfulDelete("delete-1", "iam.tenant", "tenant", "tenant-1"), successfulEntry()));

        RecycleBinItem<StandardEntity> item = new RecycleBinFacade(logService, coordinator, purgeCoordinator)
                .list(ability, PageRequest.of(1, 20)).getFirst();

        assertThat(item.sourceDeleteOperationId()).isEqualTo("delete-1");
        assertThat(item.restorable()).isTrue();
        assertThat(item.purgeable()).isFalse();
    }

    @Test
    void shouldDecorateProjectedListRecordWithoutDiscardingProjectionFields() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        Instant deletedAt = Instant.parse("2026-07-24T00:00:00Z");
        Map<String, Object> projected = Map.of(
                "id", "employee-1",
                "version", 4,
                "employeeNo", "E001",
                "organizationTitle", "戏码台",
                "title", "测试职员");
        when(ability.getModuleAlias()).thenReturn("iam.employee");
        when(ability.getDeletionEntityAlias()).thenReturn("employee");
        when(logService.latestTerminalEntry("iam.employee", "employee", "employee-1"))
                .thenReturn(new DeletionLifecycleEntry(
                        successfulDelete("delete-1", "iam.employee", "employee", "employee-1"),
                        successfulEntry()));

        RecycleBinItem<Map<String, Object>> item = new RecycleBinFacade(logService,
                mock(SoftDeleteRestoreCoordinator.class), mock(RecycleBinPurgeCoordinator.class))
                .item(ability, projected, "employee-1", deletedAt);

        assertThat(item.record()).containsEntry("organizationTitle", "戏码台");
        assertThat(item.record()).containsEntry("title", "测试职员");
        assertThat(item.deletedAt()).isEqualTo(deletedAt);
        assertThat(item.restorable()).isTrue();
    }

    @Test
    void shouldDecorateProjectedPageWithOneBatchedLifecycleRead() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        Map<String, Object> first = Map.of("id", "employee-1", "deletedAt", Instant.EPOCH);
        Map<String, Object> second = Map.of("id", "employee-2", "deletedAt", Instant.EPOCH);
        when(ability.getModuleAlias()).thenReturn("iam.employee");
        when(ability.getDeletionEntityAlias()).thenReturn("employee");
        when(logService.latestTerminalEntries("iam.employee", "employee",
                List.of("employee-1", "employee-2"))).thenReturn(Map.of(
                        "employee-1", new DeletionLifecycleEntry(
                                successfulDelete("delete-1", "iam.employee", "employee", "employee-1"),
                                successfulEntry()),
                        "employee-2", new DeletionLifecycleEntry(
                                successfulDelete("delete-2", "iam.employee", "employee", "employee-2"),
                                successfulEntry())));

        List<RecycleBinItem<Map<String, Object>>> items = new RecycleBinFacade(logService,
                mock(SoftDeleteRestoreCoordinator.class), mock(RecycleBinPurgeCoordinator.class))
                .items(ability, List.of(first, second),
                        record -> record.get("id").toString(), record -> (Instant) record.get("deletedAt"));

        assertThat(items).extracting(RecycleBinItem::sourceDeleteOperationId)
                .containsExactly("delete-1", "delete-2");
        verify(logService).latestTerminalEntries("iam.employee", "employee",
                List.of("employee-1", "employee-2"));
        verify(logService, org.mockito.Mockito.never())
                .latestTerminalEntry(any(), any(), any());
    }

    @Test
    void shouldKeepLegacyRootDeleteRecoverableWhenEntityAliasExistsOnRootEntry() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        StandardEntity record = deletedRecord("employee-1");
        DeletionOperation operation = successfulDelete("delete-1", "iam.employee", null, "employee-1");
        DeletionEntry entry = successfulEntry();
        entry.setOperationId("delete-1");
        entry.setResourceModuleAlias("iam.employee");
        entry.setResourceEntityAlias("employee");
        entry.setResourceRecordId("employee-1");
        when(ability.getModuleAlias()).thenReturn("iam.employee");
        when(ability.getDeletionEntityAlias()).thenReturn("employee");
        when(ability.listRecycleBin(any(PageRequest.class))).thenReturn(List.of(record));
        when(logService.latestTerminalEntry("iam.employee", "employee", "employee-1"))
                .thenReturn(new DeletionLifecycleEntry(operation, entry));

        RecycleBinItem<StandardEntity> item = new RecycleBinFacade(logService,
                mock(SoftDeleteRestoreCoordinator.class), mock(RecycleBinPurgeCoordinator.class))
                .list(ability, PageRequest.of(1, 20)).getFirst();

        assertThat(item.restorable()).isTrue();
        assertThat(item.sourceDeleteOperationId()).isEqualTo("delete-1");
    }

    @Test
    void shouldRejectRestoreFromAnotherResourceRoot() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        SoftDeleteRestoreCoordinator coordinator = mock(SoftDeleteRestoreCoordinator.class);
        RecycleBinPurgeCoordinator purgeCoordinator = mock(RecycleBinPurgeCoordinator.class);
        when(ability.getModuleAlias()).thenReturn("iam.tenant");
        when(ability.getDeletionEntityAlias()).thenReturn("tenant");
        when(logService.operation("delete-1")).thenReturn(successfulDelete("delete-1", "iam.role", "role", "role-1"));

        assertThatThrownBy(() -> new RecycleBinFacade(logService, coordinator, purgeCoordinator).restore(ability, "delete-1"))
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
        RecycleBinPurgeCoordinator purgeCoordinator = mock(RecycleBinPurgeCoordinator.class);
        RestoreReport expected = new RestoreReport("delete-1", "restore-1", List.of());
        when(ability.getModuleAlias()).thenReturn("iam.tenant");
        when(ability.getDeletionEntityAlias()).thenReturn("tenant");
        when(ability.canAccessRecycleBinSourceRecord("tenant-1")).thenReturn(true);
        when(logService.operation("delete-1")).thenReturn(successfulDelete("delete-1", "iam.tenant", "tenant", "tenant-1"));
        when(coordinator.restore("delete-1")).thenReturn(expected);

        RestoreReport actual = new RecycleBinFacade(logService, coordinator, purgeCoordinator).restore(ability, "delete-1");

        assertThat(actual).isSameAs(expected);
        verify(ability).beforeRecycleBinRestore();
        verify(coordinator).restore("delete-1");
    }

    @Test
    void shouldRejectRestoreWhenRetainedRootIsOutsideCurrentResourceScope() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        SoftDeleteRestoreCoordinator coordinator = mock(SoftDeleteRestoreCoordinator.class);
        RecycleBinPurgeCoordinator purgeCoordinator = mock(RecycleBinPurgeCoordinator.class);
        when(ability.getModuleAlias()).thenReturn("iam.employee");
        when(ability.getDeletionEntityAlias()).thenReturn("employee");
        when(logService.operation("delete-1"))
                .thenReturn(successfulDelete("delete-1", "iam.employee", "employee", "employee-1"));

        assertThatThrownBy(() -> new RecycleBinFacade(logService, coordinator, purgeCoordinator)
                .restore(ability, "delete-1"))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("unavailable");

        verify(coordinator, org.mockito.Mockito.never()).restore(any());
    }

    @Test
    void shouldRestoreLegacyOperationUsingMatchingRootEntryAlias() {
        @SuppressWarnings("unchecked")
        RecycleBinAbility<StandardEntity> ability = mock(RecycleBinAbility.class);
        DeletionLogService logService = mock(DeletionLogService.class);
        SoftDeleteRestoreCoordinator coordinator = mock(SoftDeleteRestoreCoordinator.class);
        RestoreReport expected = new RestoreReport("delete-1", "restore-1", List.of());
        DeletionOperation operation = successfulDelete("delete-1", "iam.employee", null, "employee-1");
        DeletionEntry rootEntry = successfulEntry();
        rootEntry.setOperationId("delete-1");
        rootEntry.setResourceModuleAlias("iam.employee");
        rootEntry.setResourceEntityAlias("employee");
        rootEntry.setResourceRecordId("employee-1");
        when(ability.getModuleAlias()).thenReturn("iam.employee");
        when(ability.getDeletionEntityAlias()).thenReturn("employee");
        when(ability.canAccessRecycleBinSourceRecord("employee-1")).thenReturn(true);
        when(logService.operation("delete-1")).thenReturn(operation);
        when(logService.operationEntries("delete-1")).thenReturn(List.of(rootEntry));
        when(coordinator.restore("delete-1")).thenReturn(expected);

        RestoreReport actual = new RecycleBinFacade(logService, coordinator,
                mock(RecycleBinPurgeCoordinator.class)).restore(ability, "delete-1");

        assertThat(actual).isSameAs(expected);
        verify(coordinator).restore("delete-1");
    }

    private static StandardEntity deletedRecord(String id) {
        StandardEntity record = new TestRecord();
        record.setId(id);
        record.setDeleted(Boolean.TRUE);
        record.setDeletedAt(Instant.parse("2026-07-24T00:00:00Z"));
        return record;
    }

    private static DeletionOperation successfulDelete(String id, String moduleAlias, String entityAlias, String recordId) {
        DeletionOperation operation = new DeletionOperation();
        operation.setId(id);
        operation.setOperationType(DeletionOperationType.DELETE);
        operation.setStatus(DeletionOperationStatus.SUCCEEDED);
        operation.setRootModuleAlias(moduleAlias);
        operation.setRootEntityAlias(entityAlias);
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
