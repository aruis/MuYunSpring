package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeAccountModelSchemaTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapEmployeeAccountDefaults() {
        TableWrapper table = mapper.toTable(EmployeeAccount.class);

        assertThat(table.getName()).isEqualTo("iam_employee_account");
        assertThat(columnDefault(table, "primary_account")).isEqualTo("FALSE");
        assertThat(columnDefault(table, "enabled")).isEqualTo("TRUE");
        assertThat(table.getIndexes())
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isTrue();
                    assertThat(index.getColumns()).containsExactly("tenant_id", "employee_id", "user_id");
                })
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isTrue();
                    assertThat(index.getColumns()).containsExactly("tenant_id", "user_id");
                });
    }

    private String columnDefault(TableWrapper table, String columnName) {
        return table.getColumns().stream()
                .filter(column -> columnName.equals(column.getName()))
                .findFirst()
                .orElseThrow()
                .getDefaultValue();
    }
}
