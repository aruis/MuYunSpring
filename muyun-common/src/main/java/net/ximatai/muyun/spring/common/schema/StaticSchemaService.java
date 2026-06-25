package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.orm.MigrationOptions;
import net.ximatai.muyun.database.core.orm.MigrationResult;
import net.ximatai.muyun.database.core.orm.SchemaManager;

import java.util.Objects;

public class StaticSchemaService {
    private final IDatabaseOperations<?> operations;
    private final StaticEntityTableMapper tableMapper;
    private final PlatformSchemaMigrationPolicy migrationPolicy;

    public StaticSchemaService(IDatabaseOperations<?> operations) {
        this(operations, new StaticEntityTableMapper());
    }

    public StaticSchemaService(IDatabaseOperations<?> operations, StaticEntityTableMapper tableMapper) {
        this(operations, tableMapper, PlatformSchemaMigrationPolicy.executeByDefault());
    }

    public StaticSchemaService(IDatabaseOperations<?> operations,
                               StaticEntityTableMapper tableMapper,
                               PlatformSchemaMigrationPolicy migrationPolicy) {
        this.operations = Objects.requireNonNull(operations, "operations must not be null");
        this.tableMapper = Objects.requireNonNull(tableMapper, "tableMapper must not be null");
        this.migrationPolicy = migrationPolicy == null
                ? PlatformSchemaMigrationPolicy.executeByDefault()
                : migrationPolicy;
    }

    public boolean ensureTable(Class<?> modelClass) {
        return ensureTable(modelClass, null).isChanged();
    }

    public MigrationResult ensureTable(Class<?> modelClass, MigrationOptions options) {
        return new SchemaManager(operations).ensureTable(tableMapper.toTable(modelClass), migrationPolicy.resolve(options));
    }
}
