package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Id;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.orm.EntityMapper;
import net.ximatai.muyun.database.core.orm.EntityMeta;
import net.ximatai.muyun.database.core.orm.EntityMetaResolver;
import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformDatabaseValueConverterTest {

    private final PlatformDatabaseValueConverter converter = new PlatformDatabaseValueConverter();

    @Test
    void shouldConvertCodeTitleEnumByCode() {
        assertThat(converter.toDatabaseValue(Status.ACTIVE)).isEqualTo("active");
        assertThat(converter.fromDatabaseValue("inactive", Status.class)).isEqualTo(Status.INACTIVE);
    }

    @Test
    void shouldFallbackToDatabaseDefaultConverter() {
        assertThat(converter.toDatabaseValue(PlainStatus.ACTIVE)).isEqualTo("ACTIVE");
        assertThat(converter.fromDatabaseValue("INACTIVE", PlainStatus.class)).isEqualTo(PlainStatus.INACTIVE);
    }

    @Test
    void shouldConvertCodeTitleEnumCollectionByCodeThroughEntityMapper() {
        EntityMeta meta = new EntityMetaResolver().resolve(StatusListEntity.class);
        StatusListEntity entity = new StatusListEntity();
        entity.id = "entity-1";
        entity.statuses = List.of(Status.ACTIVE, Status.INACTIVE, Status.ACTIVE);

        Map<String, Object> row = EntityMapper.toMap(meta, entity, false, true, converter);

        assertThat(row.get("statuses")).isEqualTo("[\"active\",\"inactive\"]");

        StatusListEntity loaded = EntityMapper.fromMap(meta, Map.of(
                "id", "entity-1",
                "statuses", "[\"active\",\"inactive\"]"
        ), StatusListEntity.class, converter);

        assertThat(loaded.statuses).containsExactly(Status.ACTIVE, Status.INACTIVE);
    }

    @Table(name = "status_list_entity")
    public static class StatusListEntity {
        @Id
        @Column(length = 32)
        private String id;

        @Column(name = "statuses", type = ColumnType.JSON_SET)
        private List<Status> statuses;
    }

    private enum Status implements CodeTitleEnum {
        ACTIVE("active", "Active"),
        INACTIVE("inactive", "Inactive");

        private final String code;
        private final String title;

        Status(String code, String title) {
            this.code = code;
            this.title = title;
        }

        @Override
        public String getCode() {
            return code;
        }

        @Override
        public String getTitle() {
            return title;
        }
    }

    private enum PlainStatus {
        ACTIVE,
        INACTIVE
    }
}
