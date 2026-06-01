package net.ximatai.muyun.spring.platform;

import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.schema.StaticEntityTableMapper;
import net.ximatai.muyun.spring.platform.application.Application;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryCategory;
import net.ximatai.muyun.spring.platform.dictionary.DictionaryItem;
import net.ximatai.muyun.spring.platform.menu.Menu;
import net.ximatai.muyun.spring.platform.menu.MenuScheme;
import net.ximatai.muyun.spring.platform.metadata.Metadata;
import net.ximatai.muyun.spring.platform.metadata.MetadataAction;
import net.ximatai.muyun.spring.platform.metadata.MetadataField;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldReferenceConfig;
import net.ximatai.muyun.spring.platform.metadata.MetadataView;
import net.ximatai.muyun.spring.platform.metadata.MetadataViewField;
import net.ximatai.muyun.spring.platform.metadata.ModuleMetadataRelation;
import net.ximatai.muyun.spring.platform.metadata.PlatformFieldType;
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
                .contains("id", "metadata_id", "field_name", "column_name", "field_type_alias", "required",
                        "unique_field", "indexed", "sortable_field", "title_field")
                .doesNotContain("dictionary_application_alias", "dictionary_category_alias", "queryable");
        assertThat(columnNames(mapper.toTable(PlatformFieldType.class)))
                .contains("id", "alias", "title", "field_type", "default_length", "default_precision",
                        "default_scale", "default_query_operator", "query_operators")
                .doesNotContain("verify_regex");
        assertThat(columnType(mapper.toTable(PlatformFieldType.class), "query_operators"))
                .isEqualTo(ColumnType.JSON_SET);
        assertThat(columnNames(mapper.toTable(MetadataFieldConfig.class)))
                .contains("id", "metadata_field_id", "relation_id", "dictionary_application_alias", "dictionary_category_alias",
                        "field_length", "precision", "scale", "queryable", "default_query_operator", "query_operators",
                        "default_value", "validation_regex", "copyable", "write_protected")
                .doesNotContain("verify_regex");
        assertThat(columnType(mapper.toTable(MetadataFieldConfig.class), "query_operators"))
                .isEqualTo(ColumnType.JSON_SET);
        assertThat(columnNames(mapper.toTable(MetadataFieldReferenceConfig.class)))
                .contains("id", "metadata_field_id", "relation_id", "target_module_alias", "target_metadata_id",
                        "cardinality", "auto_title", "title_output_field", "projection_mappings");
        assertThat(columnNames(mapper.toTable(ModuleMetadataRelation.class)))
                .contains("id", "module_alias", "metadata_id", "relation_role", "parent_metadata_id",
                        "foreign_key", "relation_alias", "auto_populate", "cascade_delete", "sort_order");
        assertThat(columnNames(mapper.toTable(MetadataView.class)))
                .contains("id", "relation_id", "view_type", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(MetadataViewField.class)))
                .contains("id", "view_id", "metadata_field_id", "visible", "control_type", "read_only",
                        "required_override", "title", "enabled", "sort_order");
        assertThat(columnNames(mapper.toTable(MetadataAction.class)))
                .contains("id", "relation_id", "action_code", "action_kind", "action_level",
                        "permission_code", "title", "enabled", "sort_order");
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

    private ColumnType columnType(TableWrapper table, String columnName) {
        return table.getColumns().stream()
                .filter(column -> columnName.equals(column.getName()))
                .findFirst()
                .orElseThrow()
                .getType();
    }
}
