package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionException;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;
import net.ximatai.muyun.spring.common.schema.StandardModelSchema;
import org.junit.jupiter.api.Test;

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

        assertThat(dynamicColumns).containsExactlyElementsOf(StandardModelSchema.columnNames());
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
                        FieldDefinition.string("name", "Name"),
                        FieldDefinition.string("title", "Title").column("name")
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
                List.of(FieldDefinition.bool("deleted", "Deleted"))
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("standard column");
    }

    @Test
    void shouldValidateModuleEntityUniqueness() {
        EntityDefinition entity = contractEntity();
        ModuleDefinition module = new ModuleDefinition(
                "contract.app",
                "Contract App",
                List.of(entity, new EntityDefinition("contract_copy", "app_contract", "Contract Copy", List.of()))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("duplicate table name");
    }

    @Test
    void shouldRequireDottedModuleAlias() {
        ModuleDefinition module = new ModuleDefinition(
                "contract_app",
                "Contract App",
                List.of(contractEntity())
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("invalid module alias");
    }

    @Test
    void shouldSupportConciseFieldFactoriesAndEntityCapabilities() {
        EntityDefinition entity = new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required().unique(),
                        FieldDefinition.titleField().required(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                        FieldDefinition.sortOrder()
                )
        ).withCapabilities(
                EntityCapability.CRUD,
                EntityCapability.SORT,
                EntityCapability.REFERENCE
        );

        TableWrapper table = mapper.toTable(entity);

        assertThat(entity.supports(EntityCapability.SORT)).isTrue();
        assertThat(table.getIndexes())
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isTrue();
                    assertThat(index.getColumns()).containsExactly("code");
                })
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isFalse();
                    assertThat(index.getColumns()).containsExactly("sort_order");
                });
    }

    @Test
    void shouldRequireCrudCapabilityForDynamicRecordEntities() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.string("code", "Code"))
        ).withCapabilities(EntityCapability.SORT))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("requires CRUD capability");
    }

    @Test
    void shouldRejectInvalidFieldTypeOptionsBeforeDdl() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.decimal("amount", "Amount").precision(2, 10))
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("scale must not exceed precision");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.bool("enabled", "Enabled").length(8))
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("length only applies");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.string("name", "Name").precision(10, 2))
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("precision and scale only apply");
    }

    @Test
    void shouldRejectInvalidSortableFields() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.string("name", "Name").sortable())
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("sortable field must be an integer type");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.sortOrder(),
                        FieldDefinition.integer("rankOrder", "Rank Order").column("rank_order").sortable()
                )
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("only have one sortable field");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.sortOrder())
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("requires SORT capability");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.string("code", "Code"))
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.SORT))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("SORT capability requires standard field sortOrder");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.integer("rankOrder", "Rank Order").column("rank_order").sortable())
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.SORT))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("SORT capability requires standard field sortOrder/sort_order");
    }

    @Test
    void shouldRejectInvalidTitleFields() {
        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.decimal("amount", "Amount").title())
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("title field must be a text type");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").title(),
                        FieldDefinition.titleField()
                )
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("only have one title field");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.titleField())
        ))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("requires REFERENCE capability");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.string("code", "Code"))
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.REFERENCE))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("REFERENCE capability requires standard field title");

        assertThatThrownBy(() -> mapper.toTable(new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(FieldDefinition.string("name", "Name").title())
        ).withCapabilities(EntityCapability.CRUD, EntityCapability.REFERENCE))).isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("REFERENCE capability requires standard field title/title");
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition(
                "contract",
                "app_contract",
                "Contract",
                List.of(
                        FieldDefinition.string("code", "Code").length(64).required().unique(),
                        FieldDefinition.string("name", "Name").length(128).required(),
                        FieldDefinition.decimal("amount", "Amount").precision(18, 2),
                        FieldDefinition.timestamp("signedAt", "Signed At").column("signed_at").indexed(),
                        FieldDefinition.bool("enabled", "Enabled")
                )
        );
    }

    private List<String> columnNames(TableWrapper table) {
        return table.getColumns().stream().map(Column::getName).toList();
    }

    private Column column(TableWrapper table, String name) {
        return table.getColumns().stream()
                .filter(column -> column.getName().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
