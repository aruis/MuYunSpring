package net.ximatai.muyun.spring.iam.employee;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeModelSchemaTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapEmployeeAsDepartmentScopedMasterData() {
        TableWrapper table = mapper.toTable(Employee.class);

        assertThat(table.getName()).isEqualTo("iam_employee");
        assertThat(columnNames(table))
                .contains("id", "tenant_id", "organization_id", "department_id", "employee_no",
                        "title", "mobile", "email", "sort_order", "enabled", "deleted", "version");
        assertThat(table.getColumns().stream().filter(column -> "organization_id".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> {
                    assertThat(column.getLength()).isEqualTo(32);
                    assertThat(column.isNullable()).isFalse();
                });
        assertThat(table.getColumns().stream().filter(column -> "department_id".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> {
                    assertThat(column.getLength()).isEqualTo(32);
                    assertThat(column.isNullable()).isFalse();
                });
        assertThat(table.getIndexes())
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isTrue();
                    assertThat(index.getColumns()).containsExactly("tenant_id", "organization_id", "employee_no");
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
