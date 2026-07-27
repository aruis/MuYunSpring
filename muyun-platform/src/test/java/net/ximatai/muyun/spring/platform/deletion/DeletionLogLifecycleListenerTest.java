package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.deletion.DeletionContext;
import net.ximatai.muyun.spring.ability.deletion.DeletionLifecycleSession;
import net.ximatai.muyun.spring.ability.deletion.DeletionMode;
import net.ximatai.muyun.spring.ability.deletion.DeletionNode;
import net.ximatai.muyun.spring.ability.deletion.DeletionResource;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeletionLogLifecycleListenerTest {
    @Test
    void shouldPersistOneOperationAndItsRecursiveEntriesWhenRootSucceeds() {
        TestMemoryDao<DeletionOperation> operationDao = new TestMemoryDao<>();
        TestMemoryDao<DeletionEntry> entryDao = new TestMemoryDao<>();
        DeletionLogLifecycleListener listener = new DeletionLogLifecycleListener(new DeletionLogService(operationDao, entryDao));
        TestOperationService service = new TestOperationService();
        DeletionOperation rootEntity = entity("tenant-1");
        DeletionOperation childEntity = entity("application-1");
        DeletionResource rootResource = new DeletionResource(service.getModuleAlias(), rootEntity.getId());
        DeletionLifecycleSession journal = listener.open(rootResource);
        DeletionContext rootContext = DeletionContext.root(service.getModuleAlias(), rootEntity.getId(), journal);

        DeletionNode rootNode = journal.started(service, rootEntity, rootContext, DeletionMode.SOFT);
        DeletionContext childContext = rootContext.child(rootNode, service.getModuleAlias(), childEntity.getId());
        DeletionNode childNode = journal.started(service, childEntity, childContext, DeletionMode.HARD);
        journal.succeeded(service, childEntity, childContext, childNode, DeletionMode.HARD);
        journal.succeeded(service, rootEntity, rootContext, rootNode, DeletionMode.SOFT);

        assertThat(operationDao.findById(rootContext.operationId()))
                .extracting(DeletionOperation::getStatus, DeletionOperation::getRootRecordId)
                .containsExactly(DeletionOperationStatus.SUCCEEDED, "tenant-1");
        assertThat(entryDao.findById(rootNode.entryId()))
                .extracting(DeletionEntry::getStatus, DeletionEntry::getTriggerType, DeletionEntry::getDeleteMode)
                .containsExactly(DeletionEntryStatus.SUCCEEDED, DeletionEntryTrigger.DIRECT, DeletionEntryMode.SOFT);
        assertThat(entryDao.findById(childNode.entryId()))
                .extracting(DeletionEntry::getParentEntryId, DeletionEntry::getStatus,
                        DeletionEntry::getTriggerType, DeletionEntry::getDeleteMode)
                .containsExactly(rootNode.entryId(), DeletionEntryStatus.SUCCEEDED,
                        DeletionEntryTrigger.CASCADE, DeletionEntryMode.HARD);
    }

    private DeletionOperation entity(String id) {
        DeletionOperation entity = new DeletionOperation();
        entity.setId(id);
        entity.setTenantId("tenant-1");
        entity.setVersion(0);
        return entity;
    }

    private static final class TestOperationService extends AbstractAbilityService<DeletionOperation> {
        private TestOperationService() {
            super("test.resource", DeletionOperation.class, new TestMemoryDao<>());
        }
    }
}
