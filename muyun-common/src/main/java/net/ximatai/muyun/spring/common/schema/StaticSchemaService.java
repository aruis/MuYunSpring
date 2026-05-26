package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.database.core.orm.SchemaManager;

import java.util.Objects;

public class StaticSchemaService {
    private final IDatabaseOperations<?> operations;
    private final StaticModelTableMapper tableMapper;

    public StaticSchemaService(IDatabaseOperations<?> operations) {
        this(operations, new StaticModelTableMapper());
    }

    public StaticSchemaService(IDatabaseOperations<?> operations, StaticModelTableMapper tableMapper) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
        this.tableMapper = Objects.requireNonNull(tableMapper, "tableMapper must not be null");
    }

    public boolean ensureTable(Class<?> modelClass) {
        return ensureTable(modelClass, MigrationOptions.execute()).isChanged();
    }

    public MigrationResult ensureTable(Class<?> modelClass, MigrationOptions options) {
        return new SchemaManager(operations).ensureTable(tableMapper.toTable(modelClass), options);
    }
}
