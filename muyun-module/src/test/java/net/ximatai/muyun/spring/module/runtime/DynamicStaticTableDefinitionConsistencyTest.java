package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.model.StandardBaseModel;
import net.ximatai.muyun.spring.common.schema.StandardModelSchema;
import net.ximatai.muyun.spring.common.schema.StaticModelTableMapper;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicStaticTableDefinitionConsistencyTest {
    @Test
    void shouldCompileStaticAndDynamicModelsToStandardPlatformTables() {
        TableWrapper staticTable = new StaticModelTableMapper().toTable(StaticContract.class);
        TableWrapper dynamicTable = new DynamicTableMapper().toTable(dynamicContract());

        assertThat(columnNames(staticTable)).containsAll(StandardModelSchema.columnNames());
        assertThat(columnNames(dynamicTable)).containsAll(StandardModelSchema.columnNames());
        assertThat(staticTable.getPrimaryKey().getName()).isEqualTo(dynamicTable.getPrimaryKey().getName());
        assertThat(staticTable.getPrimaryKey().getType()).isEqualTo(dynamicTable.getPrimaryKey().getType());
        assertThat(staticTable.getPrimaryKey().getLength()).isEqualTo(dynamicTable.getPrimaryKey().getLength());
    }

    private Set<String> columnNames(TableWrapper table) {
        Set<String> names = new LinkedHashSet<>();
        if (table.getPrimaryKey() != null) {
            names.add(table.getPrimaryKey().getName());
        }
        table.getColumns().forEach(column -> names.add(column.getName()));
        return names;
    }

    private EntityDefinition dynamicContract() {
        return new EntityDefinition(
                "contract",
                "demo_contract_dynamic",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required().unique(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                        FieldDefinition.of("enabled", FieldType.BOOLEAN, "Enabled")
                )
        );
    }

    @Table(name = "demo_contract_static", comment = "Contract")
    private static class StaticContract extends StandardBaseModel {
        @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, unique = true)
        private String code;

        @Column(name = "amount", type = ColumnType.NUMERIC, precision = 18, scale = 2)
        private java.math.BigDecimal amount;

        @Column(name = "enabled", type = ColumnType.BOOLEAN)
        private Boolean enabled;
    }
}
