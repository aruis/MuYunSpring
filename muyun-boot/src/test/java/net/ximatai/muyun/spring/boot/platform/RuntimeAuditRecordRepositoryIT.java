package net.ximatai.muyun.spring.boot.platform;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.UserTransaction;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.platform.audit.RuntimeAuditEventListener;
import net.ximatai.muyun.spring.platform.audit.RuntimeAuditRecord;
import net.ximatai.muyun.spring.platform.audit.RuntimeAuditRecordDao;
import net.ximatai.muyun.spring.platform.audit.RuntimeAuditRecordService;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@QuarkusTest
@TestProfile(RuntimeAuditRecordRepositoryIT.PostgresProfile.class)
@QuarkusTestResource(value = PostgresQuarkusTestResource.class, restrictToAnnotatedClass = true)
class RuntimeAuditRecordRepositoryIT {

    @Inject
    Config config;

    @Inject
    RuntimeAuditRecordDao auditRecordDao;

    @Inject
    RuntimeAuditRecordService service;

    @Inject
    RuntimeAuditEventListener listener;

    @Inject
    UserTransaction userTransaction;

    @Test
    void shouldPersistRuntimeAuditRecordThroughRepository() {
        requirePostgres();
        auditRecordDao.ensureTable();

        listener.onRuntimeEvent(event("audit-it-event-1"));

        RuntimeAuditRecord record = service.list(Criteria.of().eq("eventId", "audit-it-event-1"),
                        PageRequest.of(1, 10))
                .getFirst();
        assertThat(record.getTenantId()).isEqualTo("tenant-it");
        assertThat(record.getEventType()).isEqualTo(RuntimeEventType.MODULE_REFRESHED);
        assertThat(record.getModuleAlias()).isEqualTo("sales.contract");
        assertThat(record.getEntityAlias()).isNull();
        assertThat(record.getPayloadText()).contains("changed=true");
        assertThat(record.getMutationSource()).isEqualTo(RuntimeMutationSource.SYSTEM);
        assertThat(record.getSystemContext()).isTrue();
        assertThat(record.getSystemReason()).isEqualTo("repository bootstrap");
    }

    @Test
    void shouldPersistActionResultColumnsThroughRepository() {
        requirePostgres();
        auditRecordDao.ensureTable();

        listener.onRuntimeEvent(actionEvent("audit-it-action-event"));

        RuntimeAuditRecord record = service.list(Criteria.of().eq("eventId", "audit-it-action-event"),
                        PageRequest.of(1, 10))
                .getFirst();
        assertThat(record.getEventType()).isEqualTo(RuntimeEventType.ACTION_EXECUTED);
        assertThat(record.getActionCode()).isEqualTo("submit");
        assertThat(record.getExecutorType()).isEqualTo("SERVICE");
        assertThat(record.getActionLevel()).isEqualTo("RECORD");
        assertThat(record.getResultType()).isEqualTo("VALUE");
        assertThat(record.getResultMessage()).isEqualTo("提交成功");
        assertThat(record.getRefreshRequested()).isTrue();
        assertThat(record.getRedirectTo()).hasSizeGreaterThan(512);
        assertThat(record.getResultText()).isEqualTo("submitted");
    }

    @Test
    void shouldPersistActionFailureAuditWhenOuterTransactionRollsBack() throws Exception {
        requirePostgres();
        auditRecordDao.ensureTable();

        userTransaction.begin();
        try {
            listener.onRuntimeEvent(actionFailedEvent("audit-it-action-failed-rollback"));
        } finally {
            userTransaction.rollback();
        }

        RuntimeAuditRecord record = service.list(Criteria.of().eq("eventId", "audit-it-action-failed-rollback"),
                        PageRequest.of(1, 10))
                .getFirst();
        assertThat(record.getEventType()).isEqualTo(RuntimeEventType.ACTION_FAILED);
        assertThat(record.getActionCode()).isEqualTo("submit");
        assertThat(record.getExecutorType()).isEqualTo("SERVICE");
        assertThat(record.getActionLevel()).isEqualTo("RECORD");
        assertThat(record.getFailureStage()).isEqualTo("execute");
        assertThat(record.getErrorMessage()).isEqualTo("submit failed");
        assertThat(record.getErrorType()).isEqualTo(IllegalStateException.class.getName());
    }

    @Test
    void shouldRejectDuplicateRuntimeEventIdThroughRepositoryService() {
        requirePostgres();
        auditRecordDao.ensureTable();
        RuntimeEvent event = event("audit-it-event-duplicate");
        listener.onRuntimeEvent(event);

        assertThatThrownBy(() -> listener.onRuntimeEvent(event))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("Runtime audit eventId must be unique");
    }

    private RuntimeEvent event(String eventId) {
        return new RuntimeEvent(
                eventId,
                "audit-it-trace",
                RuntimeEventType.MODULE_REFRESHED,
                "sales.contract",
                null,
                null,
                null,
                "tenant-it",
                true,
                "repository bootstrap",
                RuntimeMutationSource.SYSTEM,
                Map.of("changed", true),
                Instant.parse("2026-06-02T05:00:00Z")
        );
    }

    private RuntimeEvent actionEvent(String eventId) {
        return new RuntimeEvent(
                eventId,
                "audit-it-action-trace",
                RuntimeEventType.ACTION_EXECUTED,
                "sales.contract",
                "contract",
                "contract-it-1",
                "submit",
                "tenant-it",
                false,
                RuntimeMutationSource.ACTION,
                Map.of(
                        "executorType", "SERVICE",
                        "actionLevel", "RECORD",
                        "resultType", "VALUE",
                        "message", "提交成功",
                        "refresh", true,
                        "redirectTo", "/contracts/" + "x".repeat(600),
                        "result", "submitted"
                ),
                Instant.parse("2026-06-02T05:05:00Z")
        );
    }

    private RuntimeEvent actionFailedEvent(String eventId) {
        return new RuntimeEvent(
                eventId,
                "audit-it-action-failed-trace",
                RuntimeEventType.ACTION_FAILED,
                "sales.contract",
                "contract",
                "contract-it-2",
                "submit",
                "tenant-it",
                false,
                RuntimeMutationSource.ACTION,
                Map.of(
                        "executorType", "SERVICE",
                        "actionLevel", "RECORD",
                        "available", true,
                        "failureStage", "execute",
                        "errorMessage", "submit failed",
                        "errorType", IllegalStateException.class.getName()
                ),
                Instant.parse("2026-06-02T05:10:00Z")
        );
    }

    private void requirePostgres() {
        assumeTrue(
                config.getOptionalValue("muyun.test.postgres.enabled", Boolean.class).orElse(false),
                "PostgreSQL integration test is disabled; run with -Pmuyun.postgres.it.required=true to enable it"
        );
    }

    public static class PostgresProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> config = new HashMap<>();
            config.put("quarkus.datasource.db-kind", "postgresql");
            config.put("quarkus.datasource.devservices.enabled", "false");
            config.put("quarkus.datasource.jdbc.url", "jdbc:postgresql://localhost:1/muyun_platform_it");
            config.put("quarkus.datasource.username", "testuser");
            config.put("quarkus.datasource.password", "testpass");
            config.put("muyun.database.default-schema", "public");
            config.put("muyun.database.install-postgres-plugins", "true");
            config.put("muyun.platform-bootstrap.enabled", "false");
            config.put("muyun.platform.time.default-zone-id", "Asia/Shanghai");
            config.put("quarkus.arc.exclude-types", "net.ximatai.muyun.spring.boot.web.CrudWebFormSchemaTest$*");
            config.put("quarkus.arc.remove-unused-beans", "false");
            if (Boolean.getBoolean("muyun.postgres.it.required")) {
                return config;
            }

            config.put("muyun.test.postgres.enabled", "false");
            config.put("muyun.database.repository-schema-mode", "NONE");
            return config;
        }
    }
}
