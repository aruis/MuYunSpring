package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeDelegationModelSchemaTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapEmployeeDelegationAsRelationFact() {
        TableWrapper table = mapper.toTable(EmployeeDelegation.class);

        assertThat(table.getName()).isEqualTo("iam_employee_delegation");
        assertThat(columnNames(table))
                .contains("id", "tenant_id", "delegation_type", "principal_employee_id",
                        "principal_position_id", "delegate_employee_id", "delegate_position_id",
                        "effective_from", "effective_to", "memo", "enabled", "deleted", "version");
        assertThat(table.getIndexes())
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isFalse();
                    assertThat(index.getColumns()).containsExactly("tenant_id", "principal_employee_id", "enabled");
                })
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isFalse();
                    assertThat(index.getColumns()).containsExactly("tenant_id", "delegate_employee_id", "enabled");
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
