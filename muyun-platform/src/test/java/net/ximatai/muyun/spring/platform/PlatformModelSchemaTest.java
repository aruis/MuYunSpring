package net.ximatai.muyun.spring.platform;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlatformModelSchemaTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapApplicationAsSortableEnabledPlatformModel() {
        TableWrapper table = mapper.toTable(Application.class);

        assertThat(table.getName()).isEqualTo("platform_application");
        assertThat(columnNames(table))
                .contains("id", "title", "sort_order", "enabled")
                .doesNotContain("parent_id");
    }

    @Test
    void shouldMapModuleAsTreeEnabledPlatformModel() {
        TableWrapper table = mapper.toTable(PlatformModule.class);

        assertThat(table.getName()).isEqualTo("platform_module");
        assertThat(columnNames(table))
                .contains("id", "application_alias", "title", "parent_id", "sort_order", "enabled", "module_type");
        assertThat(table.getPrimaryKey().getLength()).isEqualTo(128);
        assertThat(table.getColumns().stream().filter(column -> "parent_id".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> assertThat(column.getLength()).isEqualTo(128));
        assertThat(table.getColumns().stream().filter(column -> "module_type".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> assertThat(column.getLength()).isEqualTo(32));
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
