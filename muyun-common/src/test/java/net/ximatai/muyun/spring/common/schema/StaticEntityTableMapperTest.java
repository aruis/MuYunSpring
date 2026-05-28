package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import net.ximatai.muyun.spring.common.model.capability.EnabledCapable;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.standard.StandardTreeEntity;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticEntityTableMapperTest {
    private final StaticEntityTableMapper mapper = new StaticEntityTableMapper();

    @Test
    void shouldMapStaticPlatformModelToTableWrapper() {
        TableWrapper table = mapper.toTable(DemoOrganization.class);

        assertThat(table.getName()).isEqualTo("demo_organization");
        assertThat(table.getComment()).isEqualTo("Demo organization");
        assertThat(table.getPrimaryKey().getName()).isEqualTo("id");
        assertThat(table.getPrimaryKey().getType()).isEqualTo(ColumnType.VARCHAR);
        assertThat(table.getPrimaryKey().getLength()).isEqualTo(32);
        assertThat(columnNames(table))
                .contains("tenant_id", "version", "deleted", "deleted_at", "created_by", "created_at", "updated_by", "updated_at")
                .contains("parent_id", "code", "name", "sort_order");
        assertThat(table.getColumns().stream().filter(column -> column.getName().equals("code")).findFirst())
                .get()
                .satisfies(column -> {
                    assertThat(column.getType()).isEqualTo(ColumnType.VARCHAR);
                    assertThat(column.getLength()).isEqualTo(64);
                    assertThat(column.isNullable()).isFalse();
                });
        assertThat(table.getIndexes())
                .anySatisfy(index -> {
                    assertThat(index.isUnique()).isTrue();
                    assertThat(index.getColumns()).containsExactly("tenant_id", "code");
                });
    }

    @Test
    void shouldRejectStaticEntityOutsidePlatformBaseModel() {
        assertThatThrownBy(() -> mapper.toTable(NotPlatformModel.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("StandardEntity");
    }

    @Test
    void shouldMapStandardTreeEntityInheritedAbilityColumns() {
        TableWrapper table = mapper.toTable(DemoStandardTree.class);

        assertThat(columnNames(table))
                .contains("title", "sort_order", "parent_id");
    }

    @Test
    void shouldMapStandardEnabledTreeEntityInheritedAbilityColumns() {
        TableWrapper table = mapper.toTable(DemoStandardEnabledTree.class);

        assertThat(EnabledCapable.class).isAssignableFrom(DemoStandardEnabledTree.class);
        assertThat(columnNames(table))
                .contains("title", "sort_order", "parent_id", "enabled");
    }

    private Set<String> columnNames(TableWrapper table) {
        Set<String> names = new LinkedHashSet<>();
        if (table.getPrimaryKey() != null) {
            names.add(table.getPrimaryKey().getName());
        }
        table.getColumns().forEach(column -> names.add(column.getName()));
        return names;
    }

    @Table(name = "demo_organization", comment = "Demo organization")
    private static class DemoOrganization extends StandardEntity {
        @Column(name = "parent_id", type = ColumnType.VARCHAR, length = 32)
        private String parentId;

        @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, unique = true)
        private String code;

        @Column(name = "name", type = ColumnType.VARCHAR, length = 128, nullable = false)
        private String name;

        @Column(name = "sort_order", type = ColumnType.INT)
        private Integer sortOrder;
    }

    @Table(name = "demo_standard_tree")
    private static class DemoStandardTree extends StandardTreeEntity {
    }

    @Table(name = "demo_standard_enabled_tree")
    private static class DemoStandardEnabledTree extends StandardEnabledTreeEntity {
    }

    @Table(name = "not_platform_model")
    private static class NotPlatformModel {
        @Column(name = "name", type = ColumnType.VARCHAR)
        private String name;
    }
}
