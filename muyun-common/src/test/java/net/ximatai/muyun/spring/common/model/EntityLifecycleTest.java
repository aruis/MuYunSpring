package net.ximatai.muyun.spring.common.model;

import net.ximatai.muyun.spring.common.identity.ActingContext;
import net.ximatai.muyun.spring.common.identity.ActingContextHolder;
import net.ximatai.muyun.spring.common.identity.BusinessPrincipal;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EntityLifecycleTest {
    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
        ActingContextHolder.clear();
    }

    @Test
    void shouldPrepareStandardFieldsForInsert() {
        StandardEntity model = new TestModel();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant now = Instant.parse("2026-01-02T00:00:00Z");
        model.setCreatedAt(createdAt);

        EntityLifecycle.prepareInsert(model, now);

        assertThat(model.getId()).hasSize(32);
        assertThat(model.getVersion()).isZero();
        assertThat(model.getDeleted()).isFalse();
        assertThat(model.getDeletedAt()).isNull();
        assertThat(model.getCreatedAt()).isEqualTo(createdAt);
        assertThat(model.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void shouldPrepareStandardFieldsForUpdateAndDelete() {
        StandardEntity model = new TestModel();
        model.setVersion(3);
        Instant now = Instant.parse("2026-01-02T00:00:00Z");

        EntityLifecycle.prepareUpdate(model, now);

        assertThat(model.getVersion()).isEqualTo(4);
        assertThat(model.getUpdatedAt()).isEqualTo(now);

        EntityLifecycle.prepareDelete(model, now.plusSeconds(1));

        assertThat(model.getDeleted()).isTrue();
        assertThat(model.getDeletedAt()).isEqualTo(now.plusSeconds(1));
        assertThat(model.getVersion()).isEqualTo(5);
        assertThat(model.getUpdatedAt()).isEqualTo(now.plusSeconds(1));
    }

    @Test
    void shouldFillAuditOperatorFromCurrentUser() {
        StandardEntity model = new TestModel();
        Instant now = Instant.parse("2026-01-02T00:00:00Z");

        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("operator-1", "Operator", "tenant-a"))) {
            EntityLifecycle.prepareInsert(model, now);
        }

        assertThat(model.getCreatedBy()).isEqualTo("operator-1");
        assertThat(model.getUpdatedBy()).isEqualTo("operator-1");

        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("operator-2", "Operator", "tenant-a"))) {
            EntityLifecycle.prepareUpdate(model, now.plusSeconds(1));
        }

        assertThat(model.getCreatedBy()).isEqualTo("operator-1");
        assertThat(model.getUpdatedBy()).isEqualTo("operator-2");
    }

    @Test
    void shouldKeepAuditOperatorAsActualUserWhenActing() {
        StandardEntity model = new TestModel();
        model.setVersion(0);
        Instant now = Instant.parse("2026-01-02T00:00:00Z");
        CurrentUser operator = CurrentUser.tenantUser("assistant-user", "Assistant", "tenant-a");
        BusinessPrincipal principal = BusinessPrincipal.employee(
                "employee-principal", "org-principal", "dept-principal");

        try (CurrentUserContext.Scope user = CurrentUserContext.use(operator);
             ActingContextHolder.Scope acting = ActingContextHolder.use(new ActingContext(
                     "delegation-1", operator, principal, "sales.contract", "create"))) {
            EntityLifecycle.prepareInsert(model, now);
        }

        assertThat(model.getCreatedBy()).isEqualTo("assistant-user");
        assertThat(model.getUpdatedBy()).isEqualTo("assistant-user");

        try (CurrentUserContext.Scope user = CurrentUserContext.use(operator);
             ActingContextHolder.Scope acting = ActingContextHolder.use(new ActingContext(
                     "delegation-1", operator, principal, "sales.contract", "update"))) {
            EntityLifecycle.prepareUpdate(model, now.plusSeconds(1));
        }

        assertThat(model.getCreatedBy()).isEqualTo("assistant-user");
        assertThat(model.getUpdatedBy()).isEqualTo("assistant-user");

        try (CurrentUserContext.Scope user = CurrentUserContext.use(operator);
             ActingContextHolder.Scope acting = ActingContextHolder.use(new ActingContext(
                     "delegation-1", operator, principal, "sales.contract", "delete"))) {
            EntityLifecycle.prepareDelete(model, now.plusSeconds(2));
        }

        assertThat(model.getCreatedBy()).isEqualTo("assistant-user");
        assertThat(model.getUpdatedBy()).isEqualTo("assistant-user");
    }

    @Test
    void shouldNotClearAuditWhenCurrentUserIsMissing() {
        StandardEntity model = new TestModel();
        model.setCreatedBy("existing-creator");
        model.setUpdatedBy("existing-updater");
        Instant now = Instant.parse("2026-01-02T00:00:00Z");

        EntityLifecycle.prepareInsert(model, now);

        assertThat(model.getCreatedBy()).isEqualTo("existing-creator");
        assertThat(model.getUpdatedBy()).isEqualTo("existing-updater");

        EntityLifecycle.prepareUpdate(model, now.plusSeconds(1));

        assertThat(model.getCreatedBy()).isEqualTo("existing-creator");
        assertThat(model.getUpdatedBy()).isEqualTo("existing-updater");

        EntityLifecycle.prepareDelete(model, now.plusSeconds(2));

        assertThat(model.getCreatedBy()).isEqualTo("existing-creator");
        assertThat(model.getUpdatedBy()).isEqualTo("existing-updater");
    }

    private static final class TestModel extends StandardEntity {
    }
}
