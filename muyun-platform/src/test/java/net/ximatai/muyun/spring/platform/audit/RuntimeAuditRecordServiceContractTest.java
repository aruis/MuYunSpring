package net.ximatai.muyun.spring.platform.audit;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.common.tenant.TenantContext;
import net.ximatai.muyun.spring.platform.support.TestMemoryDao;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeAuditRecordServiceContractTest {
    private final TestMemoryDao<RuntimeAuditRecord> dao = new TestMemoryDao<>();
    private final RuntimeAuditRecordService service = new RuntimeAuditRecordService(dao);

    @Test
    void shouldPersistRuntimeEventAsAuditRecord() {
        RuntimeEvent event = event();

        String id = service.record(event);

        RuntimeAuditRecord record = service.select(id);
        assertThat(record.getId()).isNotBlank();
        assertThat(record.getEventId()).isEqualTo("event-1");
        assertThat(record.getTenantId()).isEqualTo("tenant-1");
        assertThat(record.getTraceId()).isEqualTo("trace-1");
        assertThat(record.getEventType()).isEqualTo(RuntimeEventType.ACTION_EXECUTED);
        assertThat(record.getModuleAlias()).isEqualTo("sales.contract");
        assertThat(record.getEntityAlias()).isEqualTo("contract");
        assertThat(record.getRecordId()).isEqualTo("contract-1");
        assertThat(record.getActionCode()).isEqualTo("approve");
        assertThat(record.getSystemContext()).isFalse();
        assertThat(record.getMutationSource()).isEqualTo(RuntimeMutationSource.ACTION);
        assertThat(record.getPayloadText()).contains("available=true");
        assertThat(record.getOccurredAt()).isEqualTo(Instant.parse("2026-06-02T04:00:00Z"));
    }

    @Test
    void shouldPersistThroughRuntimeAuditEventListener() {
        RuntimeAuditEventListener listener = new RuntimeAuditEventListener(service);

        listener.onRuntimeEvent(event());

        assertThat(service.list(Criteria.of().eq("eventId", "event-1"), PageRequest.of(1, 10)))
                .singleElement()
                .extracting(RuntimeAuditRecord::getTraceId)
                .isEqualTo("trace-1");
    }

    @Test
    void shouldRejectDuplicateRuntimeEventId() {
        RuntimeEvent event = event();
        service.record(event);

        assertThatThrownBy(() -> service.record(event))
                .hasMessageContaining("Runtime audit eventId must be unique");
    }

    @Test
    void shouldKeepNullEventTenantEvenWhenTenantContextExists() {
        String id;
        try (TenantContext.Scope ignored = TenantContext.use("tenant-context")) {
            id = service.record(eventWithoutTenant());
        }

        assertThat(service.select(id).getTenantId()).isNull();
    }

    private RuntimeEvent event() {
        return new RuntimeEvent(
                "event-1",
                "trace-1",
                RuntimeEventType.ACTION_EXECUTED,
                "sales.contract",
                "contract",
                "contract-1",
                "approve",
                "tenant-1",
                false,
                RuntimeMutationSource.ACTION,
                Map.of("available", true),
                Instant.parse("2026-06-02T04:00:00Z")
        );
    }

    private RuntimeEvent eventWithoutTenant() {
        return new RuntimeEvent(
                "event-without-tenant",
                "trace-without-tenant",
                RuntimeEventType.MODULE_PUBLISHED,
                "sales.contract",
                null,
                null,
                null,
                null,
                true,
                RuntimeMutationSource.SYSTEM,
                Map.of(),
                Instant.parse("2026-06-02T04:00:00Z")
        );
    }
}
