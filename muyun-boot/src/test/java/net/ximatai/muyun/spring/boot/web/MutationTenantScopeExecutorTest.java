package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MutationTenantScopeExecutorTest {
    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldNotSwitchTenantForNonSystemUser() {
        TestOwner owner = new TestOwner("tenant-b");
        TestRecord record = new TestRecord();

        try (TenantContext.Scope ignored = TenantContext.use("tenant-a")) {
            String tenantId = MutationTenantScopeExecutor.forCreate(owner, record,
                    () -> TenantContext.currentTenantId().orElseThrow());

            assertThat(tenantId).isEqualTo("tenant-a");
        }
    }

    @Test
    void shouldResolveCreateTenantForSystemUser() {
        TestOwner owner = new TestOwner("tenant-b");
        TestRecord record = new TestRecord();

        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            String tenantId = MutationTenantScopeExecutor.forCreate(owner, record,
                    () -> TenantContext.currentTenantId().orElseThrow());

            assertThat(tenantId).isEqualTo("tenant-b");
            assertThat(owner.createRecord).isSameAs(record);
        }
    }

    @Test
    void shouldResolveUpdateTenantForSystemUser() {
        TestOwner owner = new TestOwner("tenant-b");
        TestRecord record = new TestRecord();

        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            String tenantId = MutationTenantScopeExecutor.forUpdate(owner, "record-1", record,
                    () -> TenantContext.currentTenantId().orElseThrow());

            assertThat(tenantId).isEqualTo("tenant-b");
            assertThat(owner.updateId).isEqualTo("record-1");
            assertThat(owner.updateRecord).isSameAs(record);
        }
    }

    @Test
    void shouldResolveExistingRecordTenantForSystemUser() {
        TestOwner owner = new TestOwner("tenant-b");

        try (TenantContext.Scope ignored = TenantContext.system("test")) {
            String tenantId = MutationTenantScopeExecutor.forExistingRecord(owner, "record-1",
                    () -> TenantContext.currentTenantId().orElseThrow());

            assertThat(tenantId).isEqualTo("tenant-b");
            assertThat(owner.existingRecordId).isEqualTo("record-1");
        }
    }

    private static final class TestOwner implements MutationTenantScopeResolver<TestRecord> {
        private final String tenantId;
        private TestRecord createRecord;
        private String updateId;
        private TestRecord updateRecord;
        private String existingRecordId;

        private TestOwner(String tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        public Optional<String> tenantIdForCreate(TestRecord record) {
            createRecord = record;
            return Optional.ofNullable(tenantId);
        }

        @Override
        public Optional<String> tenantIdForUpdate(String id, TestRecord record) {
            updateId = id;
            updateRecord = record;
            return Optional.ofNullable(tenantId);
        }

        @Override
        public Optional<String> tenantIdForExistingRecord(String id) {
            existingRecordId = id;
            return Optional.ofNullable(tenantId);
        }
    }

    private static final class TestRecord extends StandardEntity {
    }
}
