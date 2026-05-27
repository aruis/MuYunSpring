package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.database.core.IDatabaseOperations;
import net.ximatai.muyun.database.core.builder.sql.SchemaBuildRules;
import net.ximatai.muyun.database.core.metadata.DBInfo;
import net.ximatai.muyun.database.core.orm.CompiledCriteria;
import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.CriteriaSqlCompiler;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.database.core.orm.PageResult;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DynamicRecordDao implements BaseDao<DynamicRecord, String> {
    private final IDatabaseOperations<Object> operations;
    private final EntityDefinition entity;
    private final String schema;
    private final DynamicRecordMapping mapping;
    private final CriteriaSqlCompiler criteriaSqlCompiler = new CriteriaSqlCompiler();

    @SuppressWarnings("unchecked")
    public DynamicRecordDao(IDatabaseOperations<?> operations, EntityDefinition entity) {
        this.operations = (IDatabaseOperations<Object>) Objects.requireNonNull(operations, "operations must not be null");
        new ModuleDefinitionValidator().validateEntity(entity);
        this.entity = entity;
        this.schema = operations.getDefaultSchemaName();
        this.mapping = new DynamicRecordMapping(entity);
    }

    @Override
    public boolean ensureTable() {
        throw new UnsupportedOperationException("dynamic table schema is managed by DynamicSchemaService");
    }

    @Override
    public String insert(DynamicRecord record) {
        requireSameEntity(record);
        Object id = operations.insertItem(schema, entity.tableName(), toColumnMap(record, false));
        if (id != null) {
            record.setId(String.valueOf(id));
        }
        return record.getId();
    }

    @Override
    public DynamicRecord findById(String id) {
        return loadById(id);
    }

    @Override
    public int updateById(DynamicRecord record) {
        requireSameEntity(record);
        if (record.getId() == null || record.getId().isBlank()) {
            throw new IllegalArgumentException("dynamic record id must not be blank");
        }
        if (Boolean.TRUE.equals(record.getDeleted())) {
            return operations.patchUpdateItem(schema, entity.tableName(), record.getId(), toDeleteMap(record));
        }
        return operations.patchUpdateItem(schema, entity.tableName(), record.getId(), toUpdateMap(record));
    }

    @Override
    public int updateByIdAndVersion(DynamicRecord record, Integer expectedVersion) {
        requireSameEntity(record);
        if (record.getId() == null || record.getId().isBlank()) {
            throw new IllegalArgumentException("dynamic record id must not be blank");
        }
        if (expectedVersion == null) {
            return updateById(record);
        }
        Map<String, Object> body = Boolean.TRUE.equals(record.getDeleted()) ? toDeleteMap(record) : toUpdateMap(record);
        return patchUpdateByIdAndVersion(record.getId(), record.getTenantId(), expectedVersion, body);
    }

    @Override
    public int deleteById(String id) {
        throw new UnsupportedOperationException("dynamic record delete must go through DynamicEntityService");
    }

    public boolean existsById(String id) {
        return findById(id) != null;
    }

    @Override
    public List<DynamicRecord> query(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        CompiledCriteria compiled = criteriaSqlCompiler.compile(criteria, mapping::resolveColumn, databaseType());
        StringBuilder sql = selectSql(compiled.getSql());
        appendOrderBy(sql, sorts);
        sql.append(" LIMIT :limit OFFSET :offset");

        Map<String, Object> params = new HashMap<>(compiled.getParams());
        params.put("limit", pageRequest.getLimit());
        params.put("offset", pageRequest.getOffset());

        return operations.query(sql.toString(), params).stream()
                .map(this::fromColumnMap)
                .toList();
    }

    @Override
    public PageResult<DynamicRecord> page(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        return pageQuery(criteria, pageRequest, sorts);
    }

    @Override
    public PageResult<DynamicRecord> pageQuery(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        long total = count(criteria);
        return PageResult.of(query(criteria, pageRequest, sorts), total, pageRequest);
    }

    public EntityDefinition getEntity() {
        return entity;
    }

    @Override
    public long count(Criteria criteria) {
        CompiledCriteria compiled = criteriaSqlCompiler.compile(criteria, mapping::resolveColumn, databaseType());
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total_count FROM ")
                .append(qualifiedTable());
        if (!compiled.getSql().isBlank()) {
            sql.append(" WHERE ").append(compiled.getSql());
        }
        Map<String, Object> row = operations.row(sql.toString(), compiled.getParams());
        return resolveCount(row);
    }

    @Override
    public int upsert(DynamicRecord entity) {
        throw new UnsupportedOperationException("dynamic record upsert is not supported yet");
    }

    private DynamicRecord loadById(String id) {
        return query(Criteria.of().eq(StandardEntitySchema.ID_FIELD, id), new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    private Map<String, Object> toColumnMap(DynamicRecord record, boolean includeId) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (includeId) {
            body.put(StandardEntitySchema.ID_COLUMN, record.getId());
        }
        body.put(StandardEntitySchema.TENANT_ID_COLUMN, record.getTenantId());
        body.put(StandardEntitySchema.VERSION_COLUMN, record.getVersion());
        body.put(StandardEntitySchema.DELETED_COLUMN, record.getDeleted());
        body.put(StandardEntitySchema.CREATED_BY_COLUMN, record.getCreatedBy());
        body.put(StandardEntitySchema.CREATED_AT_COLUMN, record.getCreatedAt());
        body.put(StandardEntitySchema.UPDATED_BY_COLUMN, record.getUpdatedBy());
        body.put(StandardEntitySchema.UPDATED_AT_COLUMN, record.getUpdatedAt());
        for (FieldDefinition field : entity.fields()) {
            if (record.getValues().containsKey(field.code())) {
                body.put(field.columnName(), record.getValues().get(field.code()));
            }
        }
        if (!includeId) {
            body.put(StandardEntitySchema.ID_COLUMN, record.getId());
        }
        return body;
    }

    private Map<String, Object> toUpdateMap(DynamicRecord record) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(StandardEntitySchema.VERSION_COLUMN, record.getVersion());
        body.put(StandardEntitySchema.UPDATED_AT_COLUMN, record.getUpdatedAt());
        if (record.getUpdatedBy() != null) {
            body.put(StandardEntitySchema.UPDATED_BY_COLUMN, record.getUpdatedBy());
        }
        if (record.getDeleted() != null) {
            body.put(StandardEntitySchema.DELETED_COLUMN, record.getDeleted());
        }
        for (FieldDefinition field : entity.fields()) {
            if (record.getValues().containsKey(field.code())) {
                body.put(field.columnName(), record.getValues().get(field.code()));
            }
        }
        return body;
    }

    private Map<String, Object> toDeleteMap(DynamicRecord record) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(StandardEntitySchema.DELETED_COLUMN, Boolean.TRUE);
        body.put(StandardEntitySchema.VERSION_COLUMN, record.getVersion());
        body.put(StandardEntitySchema.UPDATED_AT_COLUMN, record.getUpdatedAt());
        if (record.getUpdatedBy() != null) {
            body.put(StandardEntitySchema.UPDATED_BY_COLUMN, record.getUpdatedBy());
        }
        return body;
    }

    private int patchUpdateByIdAndVersion(String id, String tenantId, Integer expectedVersion, Map<String, Object> body) {
        Map<String, Object> params = new LinkedHashMap<>();
        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(qualifiedTable())
                .append(" SET ");
        int index = 0;
        for (Map.Entry<String, Object> entry : body.entrySet()) {
            if (index > 0) {
                sql.append(", ");
            }
            String paramName = entry.getKey();
            sql.append(quote(entry.getKey())).append(" = :").append(paramName);
            params.put(paramName, entry.getValue());
            index++;
        }
        params.put("id", id);
        params.put("expectedVersion", expectedVersion);
        sql.append(" WHERE ")
                .append(quote(StandardEntitySchema.ID_COLUMN))
                .append(" = :id AND ")
                .append(quote(StandardEntitySchema.VERSION_COLUMN))
                .append(" = :expectedVersion");
        if (tenantId != null && !tenantId.isBlank()) {
            params.put("tenantId", tenantId);
            sql.append(" AND ")
                    .append(quote(StandardEntitySchema.TENANT_ID_COLUMN))
                    .append(" = :tenantId");
        }
        return operations.update(sql.toString(), params);
    }

    private DynamicRecord fromColumnMap(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        DynamicRecord record = new DynamicRecord(entity);
        record.setId(stringValue(row.get(StandardEntitySchema.ID_COLUMN)));
        record.setTenantId(stringValue(row.get(StandardEntitySchema.TENANT_ID_COLUMN)));
        record.setVersion(numberValue(row.get(StandardEntitySchema.VERSION_COLUMN)));
        record.setDeleted((Boolean) row.get(StandardEntitySchema.DELETED_COLUMN));
        record.setCreatedBy(stringValue(row.get(StandardEntitySchema.CREATED_BY_COLUMN)));
        record.setCreatedAt(instantValue(row.get(StandardEntitySchema.CREATED_AT_COLUMN)));
        record.setUpdatedBy(stringValue(row.get(StandardEntitySchema.UPDATED_BY_COLUMN)));
        record.setUpdatedAt(instantValue(row.get(StandardEntitySchema.UPDATED_AT_COLUMN)));
        for (FieldDefinition field : entity.fields()) {
            record.putLoadedValue(field.code(), row.get(field.columnName()));
        }
        return record;
    }

    private StringBuilder selectSql(String whereSql) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(qualifiedTable());
        if (!whereSql.isBlank()) {
            sql.append(" WHERE ").append(whereSql);
        }
        return sql;
    }

    private void appendOrderBy(StringBuilder sql, Sort... sorts) {
        if (sorts == null || sorts.length == 0) {
            return;
        }
        List<String> parts = java.util.Arrays.stream(sorts)
                .filter(Objects::nonNull)
                .map(sort -> quote(mapping.resolveColumn(sort.getField())) + " " + sort.getDirection().name())
                .toList();
        if (!parts.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", parts));
        }
    }

    private String qualifiedTable() {
        return SchemaBuildRules.qualifiedName(schema, entity.tableName(), databaseType());
    }

    private String quote(String identifier) {
        return SchemaBuildRules.quoteIdentifier(identifier, databaseType());
    }

    private DBInfo.Type databaseType() {
        return operations.getDBInfo().getDatabaseType();
    }

    private long resolveCount(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return 0L;
        }
        Object value = row.get("total_count");
        if (value == null) {
            value = row.values().iterator().next();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Integer numberValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Instant instantValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof java.time.LocalDateTime localDateTime) {
            return localDateTime.toInstant(ZoneOffset.UTC);
        }
        return Instant.parse(String.valueOf(value));
    }

    private void requireSameEntity(DynamicRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        if (!entity.code().equals(record.getEntity().code())) {
            throw new IllegalArgumentException("dynamic record entity mismatch: " + record.getEntity().code());
        }
    }
}
