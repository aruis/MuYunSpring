package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SoftDeleteRestoreCoordinatorTest {
    private final TestMemoryDao<DeletionOperation> operationDao = new TestMemoryDao<>();
    private final TestMemoryDao<DeletionEntry> entryDao = new TestMemoryDao<>();
    private final DeletionLogService logService = new DeletionLogService(operationDao, entryDao);

    @Test
    void shouldWriteRestoreOperationAndLinkedEntriesForWholeSourceTree() {
        SourceTree source = completedDeleteTree();
        RestoreAbility tenant = softDeletedAbility("iam.tenant", "tenant-1");
        RestoreAbility application = softDeletedAbility("iam.tenantApplication", "application-1");

        RestoreReport report = new SoftDeleteRestoreCoordinator(logService, resolver(tenant, application))
                .restore(source.operationId());

        assertThat(report.sourceOperationId()).isEqualTo(source.operationId());
        assertThat(report.restoreOperationId()).isNotBlank();
        assertThat(report.entries()).extracting(RestoreEntryResult::status)
                .containsExactly(RestoreEntryResult.Status.RESTORED, RestoreEntryResult.Status.RESTORED);
        assertThat(logService.operation(report.restoreOperationId()))
                .extracting(DeletionOperation::getOperationType, DeletionOperation::getStatus,
                        DeletionOperation::getSourceOperationId, DeletionOperation::getRootRecordId)
                .containsExactly(DeletionOperationType.RESTORE, DeletionOperationStatus.SUCCEEDED,
                        source.operationId(), "tenant-1");
        assertThat(logService.operationEntries(report.restoreOperationId()))
                .extracting(DeletionEntry::getSourceEntryId, DeletionEntry::getParentEntryId,
                        DeletionEntry::getStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(source.rootEntryId(), null, DeletionEntryStatus.SUCCEEDED),
                        org.assertj.core.groups.Tuple.tuple(source.childEntryId(),
                                logService.operationEntries(report.restoreOperationId()).getFirst().getId(),
                                DeletionEntryStatus.SUCCEEDED));
        assertThat(tenant.record.getDeleted()).isFalse();
        assertThat(application.record.getDeleted()).isFalse();
    }

    @Test
    void shouldAuditFailedRootAndSkippedDescendants() {
        SourceTree source = completedDeleteTree();
        RestoreAbility failingTenant = new RestoreAbility("iam.tenant", "tenant-1", true);
        RestoreAbility application = softDeletedAbility("iam.tenantApplication", "application-1");

        RestoreReport report = new SoftDeleteRestoreCoordinator(logService, resolver(failingTenant, application))
                .restore(source.operationId());

        assertThat(report.entries()).extracting(RestoreEntryResult::status)
                .containsExactly(RestoreEntryResult.Status.FAILED, RestoreEntryResult.Status.SKIPPED);
        assertThat(logService.operation(report.restoreOperationId()).getStatus())
                .isEqualTo(DeletionOperationStatus.FAILED);
        assertThat(logService.operationEntries(report.restoreOperationId()))
                .extracting(DeletionEntry::getSourceEntryId, DeletionEntry::getStatus)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(source.rootEntryId(), DeletionEntryStatus.FAILED),
                        org.assertj.core.groups.Tuple.tuple(source.childEntryId(), DeletionEntryStatus.SKIPPED));
        assertThat(application.record.getDeleted()).isTrue();
    }

    @Test
    void shouldNotMarkAnAllSkippedRestoreAsSucceeded() {
        SourceTree source = completedDeleteTree();

        RestoreReport report = new SoftDeleteRestoreCoordinator(logService, List.of())
                .restore(source.operationId());

        assertThat(report.entries()).extracting(RestoreEntryResult::status)
                .containsExactly(RestoreEntryResult.Status.SKIPPED, RestoreEntryResult.Status.SKIPPED);
        assertThat(logService.operation(report.restoreOperationId()).getStatus())
                .isEqualTo(DeletionOperationStatus.PARTIALLY_SUCCEEDED);
    }

    @Test
    void shouldIgnoreUnboundSoftDeleteAbilitiesWhenIndexingStaticRecoveryResources() {
        RestoreAbility tenant = softDeletedAbility("iam.tenant", "tenant-1");
        SoftDeleteAbility<TestRecord> unbound = new SoftDeleteAbility<>() {
            @Override
            public net.ximatai.muyun.spring.ability.BaseDao<TestRecord, String> getDao() {
                return new TestMemoryDao<>();
            }

            @Override
            public String getModuleAlias() {
                return null;
            }
        };
        StaticDeletionRecoveryResourceResolver resolver =
                new StaticDeletionRecoveryResourceResolver(List.of(tenant, unbound, unbound));
        DeletionEntry entry = new DeletionEntry();
        entry.setResourceModuleAlias("iam.tenant");

        assertThat(resolver.resolve(entry)).containsSame(tenant);
    }

    private SourceTree completedDeleteTree() {
        DeletionOperation operation = new DeletionOperation();
        operation.setTenantId("tenant-1");
        operation.setOperationType(DeletionOperationType.DELETE);
        operation.setRootModuleAlias("iam.tenant");
        operation.setRootRecordId("tenant-1");
        String operationId = logService.startOperation(operation);
        String rootEntryId = startEntry(operationId, null, "iam.tenant", "tenant-1");
        String childEntryId = startEntry(operationId, rootEntryId, "iam.tenantApplication", "application-1");
        logService.completeEntry(rootEntryId, DeletionEntryStatus.SUCCEEDED, null);
        logService.completeEntry(childEntryId, DeletionEntryStatus.SUCCEEDED, null);
        logService.completeOperation(operationId, DeletionOperationStatus.SUCCEEDED, null);
        return new SourceTree(operationId, rootEntryId, childEntryId);
    }

    private String startEntry(String operationId, String parentEntryId, String moduleAlias, String recordId) {
        DeletionEntry entry = new DeletionEntry();
        entry.setTenantId("tenant-1");
        entry.setOperationId(operationId);
        entry.setParentEntryId(parentEntryId);
        entry.setResourceModuleAlias(moduleAlias);
        entry.setResourceRecordId(recordId);
        entry.setTriggerType(parentEntryId == null ? DeletionEntryTrigger.DIRECT : DeletionEntryTrigger.CASCADE);
        entry.setDeleteMode(DeletionEntryMode.SOFT);
        return logService.startEntry(entry);
    }

    private RestoreAbility softDeletedAbility(String moduleAlias, String id) {
        return new RestoreAbility(moduleAlias, id, false);
    }

    private List<DeletionRecoveryResourceResolver> resolver(RestoreAbility... abilities) {
        return List.of(entry -> java.util.Arrays.stream(abilities)
                .filter(ability -> ability.getModuleAlias().equals(entry.getResourceModuleAlias()))
                .findFirst()
                .map(ability -> (SoftDeleteAbility<?>) ability));
    }

    private static final class RestoreAbility extends AbstractAbilityService<TestRecord>
            implements SoftDeleteAbility<TestRecord> {
        private final TestRecord record;
        private final boolean fails;

        private RestoreAbility(String moduleAlias, String id, boolean fails) {
            super(moduleAlias, TestRecord.class, new TestMemoryDao<>());
            this.fails = fails;
            this.record = new TestRecord();
            record.setId(id);
            record.setTenantId("tenant-1");
            record.setVersion(0);
            record.setDeleted(true);
            getDao().insert(record);
        }

        @Override
        public int restore(String id) {
            if (fails) {
                throw new IllegalStateException("restore rejected");
            }
            return SoftDeleteAbility.super.restore(id);
        }
    }

    private static final class TestRecord extends StandardEntity {
    }

    private record SourceTree(String operationId, String rootEntryId, String childEntryId) {
    }
}
