package net.ximatai.muyun.spring.iam.position;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PositionModelSchemaTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapPositionAsTenantScopedMasterData() {
        TableWrapper table = mapper.toTable(Position.class);

        assertThat(table.getName()).isEqualTo("iam_position");
        assertThat(columnNames(table))
                .contains("id", "tenant_id", "code", "title", "description",
                        "sort_order", "enabled", "deleted", "version");
        assertThat(table.getColumns().stream().filter(column -> "code".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> {
                    assertThat(column.getLength()).isEqualTo(64);
                    assertThat(column.isNullable()).isFalse();
                });
        assertThat(table.getIndexes())
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isTrue();
                    assertThat(index.getColumns()).containsExactly("tenant_id", "code");
                });
    }

    private Set<String> columnNames(TableWrapper table) {
        Set<String> names = new LinkedHashSet<>();
        if (table.getPrimaryKey() != null) {
            names.add(table.getPrimaryKey().getName());
        }
        table.getColumns().forEach(column -> names.add(column.getName()));
        return names;
    }
}
