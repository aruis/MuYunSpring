package net.ximatai.muyun.spring.iam.tenant;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TenantModelSchemaTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapTenantAsGlobalIdentityRootModel() {
        TableWrapper table = mapper.toTable(Tenant.class);

        assertThat(table.getName()).isEqualTo("iam_tenant");
        assertThat(columnNames(table))
                .contains("id", "tenant_id", "title", "sort_order", "enabled")
                .doesNotContain("parent_id");
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
