package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.IMetaDataLoader;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.metadata.DBIndex;
import net.ximatai.muyun.database.core.metadata.DBColumn;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.spring.common.model.StandardEntity;
import org.junit.jupiter.api.Test;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StaticSchemaServiceTest {
    @Test
    void shouldEnsureStaticEntityTableThroughUnifiedSchemaManager() {
        FakeOperations operations = new FakeOperations();
        StaticSchemaService service = new StaticSchemaService(operations);

        MigrationResult result = service.ensureTable(DemoStaticEntity.class, MigrationOptions.dryRun());

        assertThat(result.isChanged()).isTrue();
        assertThat(result.isDryRun()).isTrue();
        assertThat(result.getStatements()).anySatisfy(sql -> assertThat(sql).contains("demo_static_model"));
        assertThat(operations.executedSql).isEmpty();
    }

    @Table(name = "demo_static_model", comment = "Demo static model")
    private static class DemoStaticEntity extends StandardEntity {
        @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false)
        private String code;
    }

    private static class FakeMetaDataLoader implements IMetaDataLoader {
        private final DBInfo info = new DBInfo("POSTGRESQL");

        @Override
        public DBInfo getDBInfo() {
            return info;
        }

        @Override
        public void resetInfo() {
        }

        @Override
        public List<DBIndex> getIndexList(String schema, String table) {
            return List.of();
        }

        @Override
        public Map<String, DBColumn> getColumnMap(String schema, String table) {
            return Map.of();
        }
    }

    private static class FakeOperations implements IDatabaseOperations<Object> {
        private final FakeMetaDataLoader loader = new FakeMetaDataLoader();
        private final List<String> executedSql = new ArrayList<>();

        @Override
        public IMetaDataLoader getMetaDataLoader() {
            return loader;
        }

        @Override
        public String getPKName() {
            return "id";
        }

        @Override
        public Object insert(String sql, Map<String, Object> params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object insertWithPK(String sql, Map<String, Object> params, Object pk) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Object> batchInsert(String sql, List<Map<String, Object>> paramsList) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, Object> row(String sql, List<Object> params) {
            return null;
        }

        @Override
        public Map<String, Object> row(String sql, Map<String, Object> params) {
            return null;
        }

        @Override
        public List<Map<String, Object>> query(String sql, Map<String, Object> params) {
            return List.of();
        }

        @Override
        public List<Map<String, Object>> query(String sql, List<Object> params) {
            return List.of();
        }

        @Override
        public int update(String sql, Map<String, Object> params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int update(String sql, List<Object> params) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int execute(String sql) {
            executedSql.add(sql);
            return 1;
        }

        @Override
        public int execute(String sql, Object... params) {
            executedSql.add(sql);
            return 1;
        }

        @Override
        public int execute(String sql, List<Object> params) {
            executedSql.add(sql);
            return 1;
        }

        @Override
        public Array createArray(List<Object> list, String type) {
            throw new UnsupportedOperationException();
        }
    }
}
