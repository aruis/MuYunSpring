package net.ximatai.muyun.spring.platform;

import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.module.PlatformModule;
import org.junit.jupiter.api.Test;

import java.util.List;
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
                .contains("id", "application_alias", "title", "parent_id", "sort_order", "enabled", "module_kind");
        assertThat(table.getPrimaryKey().getLength()).isEqualTo(128);
        assertThat(table.getColumns().stream().filter(column -> "parent_id".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> assertThat(column.getLength()).isEqualTo(128));
        assertThat(table.getColumns().stream().filter(column -> "module_kind".equals(column.getName())).findFirst())
                .get()
                .satisfies(column -> assertThat(column.getLength()).isEqualTo(32));
    }

    @Test
    void shouldMapMetadataModelsAsPlatformTables() {
        assertThat(columnNames(mapper.toTable(Metadata.class)))
                .contains("id", "application_alias", "alias", "schema_name", "table_name", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(MetadataField.class)))
                .contains("id", "metadata_id", "field_name", "column_name", "field_type", "required",
                        "unique_field", "indexed", "sortable_field", "title_field", "field_length",
                        "dictionary_application_alias", "dictionary_category_alias");
        assertThat(columnNames(mapper.toTable(ModuleMetadataRelation.class)))
                .contains("id", "module_alias", "metadata_id", "relation_role", "parent_metadata_id",
                        "foreign_key", "relation_alias", "auto_populate", "cascade_delete", "sort_order");
    }

    @Test
    void shouldMapMenuModelsAsPlatformTables() {
        assertThat(columnNames(mapper.toTable(MenuScheme.class)))
                .contains("id", "tenant_id", "alias", "scope_type", "scope_id", "title", "enabled", "sort_order")
                .doesNotContain("parent_id", "application_alias");
        assertThat(columnNames(mapper.toTable(Menu.class)))
                .contains("id", "tenant_id", "scheme_id", "parent_id", "menu_type", "module_alias",
                        "route", "external_url", "title", "enabled", "sort_order")
                .doesNotContain("application_alias");
    }

    @Test
    void shouldMapDictionaryModelsAsPlatformTables() {
        assertThat(columnNames(mapper.toTable(DictionaryCategory.class)))
                .contains("id", "tenant_id", "application_alias", "alias", "category_kind",
                        "parent_id", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(DictionaryItem.class)))
                .contains("id", "tenant_id", "application_alias", "category_alias", "code",
                        "parent_id", "title", "enabled", "sort_order");
        assertThat(uniqueIndexes(mapper.toTable(DictionaryCategory.class)))
                .contains(List.of("tenant_id", "application_alias", "alias"));
        assertThat(uniqueIndexes(mapper.toTable(DictionaryItem.class)))
                .contains(List.of("tenant_id", "application_alias", "category_alias", "code"));
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
}
