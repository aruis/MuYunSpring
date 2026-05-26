package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldType;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;
import net.ximatai.muyun.spring.common.model.StandardBaseModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicTableMapperTest {
    private final DynamicTableMapper mapper = new DynamicTableMapper();
    private final ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

    @Test
    void shouldMapDynamicEntityToTableWithStandardColumnsAndIndexes() {
        EntityDefinition entity = contractEntity();

        TableWrapper table = mapper.toTable(entity);

        assertThat(table.getName()).isEqualTo("app_contract");
        assertThat(table.getComment()).isEqualTo("Contract");
        assertThat(table.getPrimaryKey().getName()).isEqualTo("id");
        assertThat(columnNames(table))
                .containsExactly(
                        "version", "deleted", "created_by", "created_at", "updated_by", "updated_at",
                        "code", "name", "amount", "signed_at", "enabled"
                );
        assertThat(table.getIndexes())
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isTrue();
                    assertThat(index.getColumns()).containsExactly("code");
                })
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isFalse();
                    assertThat(index.getColumns()).containsExactly("signed_at");
                });
        assertThat(column(table, "name").isNullable()).isFalse();
        assertThat(column(table, "name").getLength()).isEqualTo(128);
        assertThat(column(table, "amount").getPrecision()).isEqualTo(18);
        assertThat(column(table, "amount").getScale()).isEqualTo(2);
    }

    @Test
    void shouldKeepDynamicStandardColumnsAlignedWithStaticBaseModel() {
        TableWrapper table = mapper.toTable(contractEntity());

        List<String> dynamicColumns = new java.util.ArrayList<>();
        dynamicColumns.add(table.getPrimaryKey().getName());
        dynamicColumns.addAll(columnNames(table).stream()
                .filter(name -> name.equals("version")
                        || name.equals("deleted")
                        || name.equals("created_by")
                        || name.equals("created_at")
                        || name.equals("updated_by")
                        || name.equals("updated_at"))
                .toList());

        assertThat(dynamicColumns).containsExactlyElementsOf(staticBaseModelColumns());
    }

    @Test
    void shouldRejectUnsafeIdentifiersAndDuplicateColumns() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract;drop",
                "Contract",
                List.of()
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid table name");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        new FieldDefinition("name", "name", FieldType.STRING, "Name"),
                        new FieldDefinition("title", "name", FieldType.STRING, "Title")
                )
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("duplicate column name");
    }

    @Test
    void shouldRejectFieldsThatConflictWithStandardColumns() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(new FieldDefinition("deleted", "deleted", FieldType.BOOLEAN, "Deleted"))
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("standard column");
    }

    @Test
    void shouldValidateModuleEntityUniqueness() {
        EntityDefinition entity = contractEntity();
        ModuleDefinition module = new ModuleDefinition(
                "contract_app",
                "Contract App",
                List.of(entity, new EntityDefinition("contract_copy", "app_contract", "Contract Copy", List.of()))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("duplicate table name");
    }

    @Test
    void shouldRejectInvalidFieldTypeOptionsBeforeDdl() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(new FieldDefinition("amount", "amount", FieldType.DECIMAL, "Amount", false, false, false, false, false, null, 2, 10))
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("scale must not exceed precision");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(new FieldDefinition("enabled", "enabled", FieldType.BOOLEAN, "Enabled").length(8))
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("length only applies");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(new FieldDefinition("name", "name", FieldType.STRING, "Name", false, false, false, false, false, null, 10, 2))
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("precision and scale only apply");
    }

    @Test
    void shouldRejectInvalidSortableFields() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(new FieldDefinition("name", "name", FieldType.STRING, "Name").asSortable())
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("sortable field must be an integer type");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        new FieldDefinition("sort_order", "sort_order", FieldType.INTEGER, "Sort Order").asSortable(),
                        new FieldDefinition("rank_order", "rank_order", FieldType.INTEGER, "Rank Order").asSortable()
                )
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("only have one sortable field");
    }

    @Test
    void shouldRejectInvalidTitleFields() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(new FieldDefinition("amount", "amount", FieldType.DECIMAL, "Amount").asTitle())
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("title field must be a text type");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        new FieldDefinition("code", "code", FieldType.STRING, "Code").asTitle(),
                        new FieldDefinition("name", "name", FieldType.STRING, "Name").asTitle()
                )
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("only have one title field");
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        new FieldDefinition("code", "code", FieldType.STRING, "Code").length(64).asRequired().asUnique(),
                        new FieldDefinition("name", "name", FieldType.STRING, "Name").length(128).asRequired(),
                        new FieldDefinition("amount", "amount", FieldType.DECIMAL, "Amount").precision(18, 2),
                        new FieldDefinition("signed_at", "signed_at", FieldType.TIMESTAMP, "Signed At").asIndexed(),
                        new FieldDefinition("enabled", "enabled", FieldType.BOOLEAN, "Enabled")
                )
        );
    }

    private List<String> columnNames(TableWrapper table) {
        return table.getColumns().stream().map(Column::getName).toList();
    }

    private List<String> staticBaseModelColumns() {
        return java.util.Arrays.stream(StandardBaseModel.class.getDeclaredFields())
                .map(this::columnName)
                .toList();
    }

    private String columnName(Field field) {
        net.ximatai.muyun.database.core.annotation.Column column =
                field.getAnnotation(net.ximatai.muyun.database.core.annotation.Column.class);
        return column.name();
    }

    private Column column(TableWrapper table, String name) {
        return table.getColumns().stream()
                .filter(column -> column.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
