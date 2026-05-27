package net.ximatai.muyun.spring.common.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EntityLifecycleTest {
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

    private static final class TestModel extends StandardEntity {
    }
}
