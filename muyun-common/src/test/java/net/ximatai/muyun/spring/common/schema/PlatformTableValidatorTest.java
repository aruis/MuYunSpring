package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

import net.ximatai.muyun.database.core.builder.Column;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.builder.TableWrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlatformTableValidatorTest {
    private final PlatformTableValidator validator = new PlatformTableValidator();

    @Test
    void shouldRejectMissingStandardColumn() {
        TableWrapper table = baseTable();

        assertThatThrownBy(() -> validator.requireStandardEntityTable(table, "demo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing standard column tenant_id");
    }

    @Test
    void shouldRejectMissingDeletedAtColumn() {
        TableWrapper table = baseTable();
        StandardEntitySchema.auditColumns().stream()
                .filter(column -> !"deleted_at".equals(column.getName()))
                .forEach(table::addColumn);

        assertThatThrownBy(() -> validator.requireStandardEntityTable(table, "demo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing standard column deleted_at");
    }

    @Test
    void shouldRejectIdThatIsNotPrimaryKey() {
        TableWrapper table = TableWrapper.withName("demo")
                .addColumn(Column.of("id").setType(ColumnType.VARCHAR).setLength(32).setNullable(false));
        StandardEntitySchema.auditColumns().forEach(table::addColumn);

        assertThatThrownBy(() -> validator.requireStandardEntityTable(table, "demo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be primary key id");
    }

    @Test
    void shouldAcceptLongerPrimaryKeyForModelSpecificSemanticId() {
        TableWrapper table = TableWrapper.withName("demo")
                .setPrimaryKey(Column.of("id").setType(ColumnType.VARCHAR).setLength(128).setNullable(false));
        StandardEntitySchema.auditColumns().forEach(table::addColumn);

        assertThatCode(() -> validator.requireStandardEntityTable(table, "demo"))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectStandardColumnTypeMismatch() {
        TableWrapper table = baseTable()
                .addColumn(Column.of("version").setType(ColumnType.VARCHAR));
        StandardEntitySchema.auditColumns().stream()
                .filter(column -> !"version".equals(column.getName()))
                .forEach(table::addColumn);

        assertThatThrownBy(() -> validator.requireStandardEntityTable(table, "demo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type mismatch version");
    }

    @Test
    void shouldRejectStandardColumnSizeMismatch() {
        TableWrapper table = baseTable()
                .addColumn(Column.of("version").setType(ColumnType.INT).setLength(32));
        StandardEntitySchema.auditColumns().stream()
                .filter(column -> !"version".equals(column.getName()))
                .forEach(table::addColumn);

        assertThatThrownBy(() -> validator.requireStandardEntityTable(table, "demo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size mismatch version");
    }

    private TableWrapper baseTable() {
        return TableWrapper.withName("demo")
                .setPrimaryKey(StandardEntitySchema.idColumn());
    }
}
