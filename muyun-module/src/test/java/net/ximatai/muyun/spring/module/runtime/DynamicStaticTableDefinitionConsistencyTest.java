package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicStaticTableDefinitionConsistencyTest {
    @Test
    void shouldCompileStaticAndDynamicModelsToStandardPlatformTables() {
        TableWrapper staticTable = new StaticEntityTableMapper().toTable(StaticContract.class);
        TableWrapper dynamicTable = new DynamicTableMapper().toTable(dynamicContract());

        assertThat(columnNames(staticTable)).containsAll(StandardEntitySchema.columnNames());
        assertThat(columnNames(dynamicTable)).containsAll(StandardEntitySchema.columnNames());
        assertThat(staticTable.getPrimaryKey().getName()).isEqualTo(dynamicTable.getPrimaryKey().getName());
        assertThat(staticTable.getPrimaryKey().getType()).isEqualTo(dynamicTable.getPrimaryKey().getType());
        assertThat(staticTable.getPrimaryKey().getLength()).isEqualTo(dynamicTable.getPrimaryKey().getLength());
        assertThat(uniqueIndexes(staticTable)).contains(List.of("tenant_id", "code"));
        assertThat(uniqueIndexes(dynamicTable)).contains(List.of("tenant_id", "code"));
    }

    private Set<String> columnNames(TableWrapper table) {
        Set<String> names = new LinkedHashSet<>();
        if (table.getPrimaryKey() != null) {
            names.add(table.getPrimaryKey().getName());
        }
        table.getColumns().forEach(column -> names.add(column.getName()));
        return names;
    }

    private List<List<String>> uniqueIndexes(TableWrapper table) {
        return table.getIndexes().stream()
                .filter(index -> index.isUnique())
                .map(index -> List.copyOf(index.getColumns()))
                .toList();
    }

    private EntityDefinition dynamicContract() {
        return new EntityDefinition(
                "contract",
                "demo_contract_dynamic",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required().unique(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                        FieldDefinition.enabled()
                )
        ).withCapabilities(EntityCapability.ENABLE);
    }

    @Table(name = "demo_contract_static", comment = "Contract")
    private static class StaticContract extends StandardEntity {
        @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, unique = true)
        private String code;

        @Column(name = "amount", type = ColumnType.NUMERIC, precision = 18, scale = 2)
        private java.math.BigDecimal amount;

        @Column(name = "enabled", type = ColumnType.BOOLEAN)
        private Boolean enabled;
    }
}
