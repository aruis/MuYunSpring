package net.ximatai.muyun.spring.platform.audit;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.spring.boot.sql.annotation.EnableMuYunRepositories;
import net.ximatai.muyun.spring.ability.event.RuntimeEvent;
import net.ximatai.muyun.spring.ability.event.RuntimeEventType;
import net.ximatai.muyun.spring.ability.event.RuntimeMutationSource;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = RuntimeAuditRecordRepositoryIT.TestApplication.class)
class RuntimeAuditRecordRepositoryIT {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("muyun.database.repository-schema-mode", () -> "ENSURE");
    }

    private final RuntimeAuditRecordService service;
    private final RuntimeAuditEventListener listener;

    @Autowired
    RuntimeAuditRecordRepositoryIT(RuntimeAuditRecordService service, RuntimeAuditEventListener listener) {
        this.service = service;
        this.listener = listener;
    }

    @Test
    void shouldPersistRuntimeAuditRecordThroughRepository() {
        listener.onRuntimeEvent(event("audit-it-event-1"));

        RuntimeAuditRecord record = service.list(Criteria.of().eq("eventId", "audit-it-event-1"),
                        PageRequest.of(1, 10))
                .getFirst();
        assertThat(record.getTenantId()).isEqualTo("tenant-it");
        assertThat(record.getEventType()).isEqualTo(RuntimeEventType.MODULE_PUBLISHED);
        assertThat(record.getModuleAlias()).isEqualTo("sales.contract");
        assertThat(record.getEntityAlias()).isNull();
        assertThat(record.getPayloadText()).contains("changed=true");
        assertThat(record.getMutationSource()).isEqualTo(RuntimeMutationSource.SYSTEM);
    }

    @Test
    void shouldRejectDuplicateRuntimeEventIdThroughRepositoryService() {
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
                RuntimeEventType.MODULE_PUBLISHED,
                "sales.contract",
                null,
                null,
                null,
                "tenant-it",
                true,
                RuntimeMutationSource.SYSTEM,
                Map.of("changed", true),
                Instant.parse("2026-06-02T05:00:00Z")
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableMuYunRepositories(basePackageClasses = RuntimeAuditRecordDao.class)
    static class TestApplication {
        @Bean
        DataSource dataSource() {
            return DataSourceBuilder.create()
                    .url(postgres.getJdbcUrl())
                    .username(postgres.getUsername())
                    .password(postgres.getPassword())
                    .driverClassName(postgres.getDriverClassName())
                    .build();
        }

        @Bean
        RuntimeAuditRecordService runtimeAuditRecordService(RuntimeAuditRecordDao auditRecordDao) {
            return new RuntimeAuditRecordService(auditRecordDao);
        }

        @Bean
        RuntimeAuditEventListener runtimeAuditEventListener(RuntimeAuditRecordService service) {
            return new RuntimeAuditEventListener(service);
        }
    }
}
