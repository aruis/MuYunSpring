package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectedRecordValuesTest {
    @Test
    void shouldReadIdentityFieldsFromEntityAndMapProjection() {
        StandardEntity entity = new StandardEntity() { };
        entity.setId("entity-1");
        entity.setDeletedAt(Instant.EPOCH);

        assertThat(ProjectedRecordValues.id(entity)).isEqualTo("entity-1");
        assertThat(ProjectedRecordValues.deletedAt(entity)).isEqualTo(Instant.EPOCH);

        LocalDateTime localDeletedAt = LocalDateTime.of(2026, 7, 27, 12, 0);
        Map<String, Object> projection = Map.of("id", "projected-1", "deletedAt", localDeletedAt);
        assertThat(ProjectedRecordValues.id(projection)).isEqualTo("projected-1");
        assertThat(ProjectedRecordValues.deletedAt(projection))
                .isEqualTo(localDeletedAt.atZone(ZoneId.systemDefault()).toInstant());
    }
}
