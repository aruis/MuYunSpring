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
import net.ximatai.muyun.spring.common.id.Ids;
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

public class DynamicRecordDao {
    private final IDatabaseOperations<Object> operations;
    private final EntityDefinition entity;
    private final String schema;
    private final DynamicRecordMapping mapping;
    private final DynamicRecordLifecycle lifecycle;
    private final CriteriaSqlCompiler criteriaSqlCompiler = new CriteriaSqlCompiler();

    @SuppressWarnings("unchecked")
    public DynamicRecordDao(IDatabaseOperations<?> operations, EntityDefinition entity) {
        this(operations, entity, DynamicRecordLifecycle.NONE);
    }

    @SuppressWarnings("unchecked")
    public DynamicRecordDao(IDatabaseOperations<?> operations, EntityDefinition entity, DynamicRecordLifecycle lifecycle) {
        this.operations = (IDatabaseOperations<Object>) Objects.requireNonNull(operations, "operations must not be null");
        new ModuleDefinitionValidator().validateEntity(entity);
        this.entity = entity;
        this.schema = operations.getDefaultSchemaName();
        this.mapping = new DynamicRecordMapping(entity);
        this.lifecycle = lifecycle == null ? DynamicRecordLifecycle.NONE : lifecycle;
    }

    public String insert(DynamicRecord record) {
        requireSameEntity(record);
        if (record.getId() == null || record.getId().isBlank()) {
            record.setId(Ids.newId());
        }
        Instant now = Instant.now();
        record.setVersion(record.getVersion() == null ? 0 : record.getVersion());
        record.setDeleted(Boolean.FALSE);
        record.setCreatedAt(record.getCreatedAt() == null ? now : record.getCreatedAt());
        record.setUpdatedAt(now);
        lifecycle.beforeInsert(record);
        record.validateForInsert();

        Object id = operations.insertItem(schema, entity.tableName(), toColumnMap(record, false));
        if (id != null) {
            record.setId(String.valueOf(id));
        }
        return record.getId();
    }

    public DynamicRecord findById(String id) {
        DynamicRecord record = loadActiveById(id);
        if (record != null) {
            lifecycle.afterSelect(record);
        }
        return record;
    }

    public int update(DynamicRecord record) {
        requireSameEntity(record);
        if (record.getId() == null || record.getId().isBlank()) {
            throw new IllegalArgumentException("dynamic record id must not be blank");
        }
        record.setUpdatedAt(Instant.now());
        record.setVersion(nextVersion(record));
        lifecycle.beforeUpdate(record);
        return operations.patchUpdateItem(schema, entity.tableName(), record.getId(), toUpdateMap(record));
    }

    public int delete(String id) {
        lifecycle.beforeDelete(id);
        DynamicRecord record = loadActiveById(id);
        if (record == null) {
            return 0;
        }
        record.setDeleted(Boolean.TRUE);
        record.setUpdatedAt(Instant.now());
        record.setVersion(record.getVersion() == null ? 1 : record.getVersion() + 1);
        return operations.patchUpdateItem(schema, entity.tableName(), record.getId(), toDeleteMap(record));
    }

    public List<DynamicRecord> query(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        Objects.requireNonNull(pageRequest, "pageRequest must not be null");
        CompiledCriteria compiled = criteriaSqlCompiler.compile(activeCriteria(criteria), mapping::resolveColumn, databaseType());
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

    public PageResult<DynamicRecord> page(Criteria criteria, PageRequest pageRequest, Sort... sorts) {
        long total = count(criteria);
        return PageResult.of(query(criteria, pageRequest, sorts), total, pageRequest);
    }

    public long count(Criteria criteria) {
        CompiledCriteria compiled = criteriaSqlCompiler.compile(activeCriteria(criteria), mapping::resolveColumn, databaseType());
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) AS total_count FROM ")
                .append(qualifiedTable());
        if (!compiled.getSql().isBlank()) {
            sql.append(" WHERE ").append(compiled.getSql());
        }
        Map<String, Object> row = operations.row(sql.toString(), compiled.getParams());
        return resolveCount(row);
    }

    private Criteria activeCriteria(Criteria criteria) {
        Criteria scoped = Criteria.of();
        if (criteria != null && !criteria.isEmpty()) {
            scoped.andGroup(criteria.getRoot());
        }
        scoped.andGroup(group -> group.eq("deleted", Boolean.FALSE).orIsNull("deleted"));
        return scoped;
    }

    private DynamicRecord loadActiveById(String id) {
        return query(Criteria.of().eq("id", id), new PageRequest(0, 1)).stream().findFirst().orElse(null);
    }

    private Map<String, Object> toColumnMap(DynamicRecord record, boolean includeId) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (includeId) {
            body.put("id", record.getId());
        }
        body.put("version", record.getVersion());
        body.put("deleted", record.getDeleted());
        body.put("created_by", record.getCreatedBy());
        body.put("created_at", record.getCreatedAt());
        body.put("updated_by", record.getUpdatedBy());
        body.put("updated_at", record.getUpdatedAt());
        for (FieldDefinition field : entity.fields()) {
            if (record.getValues().containsKey(field.code())) {
                body.put(field.columnName(), record.getValues().get(field.code()));
            }
        }
        if (!includeId) {
            body.put("id", record.getId());
        }
        return body;
    }

    private int nextVersion(DynamicRecord record) {
        if (record.getVersion() != null) {
            return record.getVersion() + 1;
        }
        DynamicRecord current = loadActiveById(record.getId());
        if (current == null) {
            throw new IllegalArgumentException("dynamic record not found: " + record.getId());
        }
        return current.getVersion() == null ? 1 : current.getVersion() + 1;
    }

    private Map<String, Object> toUpdateMap(DynamicRecord record) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("version", record.getVersion());
        body.put("updated_at", record.getUpdatedAt());
        if (record.getUpdatedBy() != null) {
            body.put("updated_by", record.getUpdatedBy());
        }
        if (record.getDeleted() != null) {
            body.put("deleted", record.getDeleted());
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
        body.put("deleted", Boolean.TRUE);
        body.put("version", record.getVersion());
        body.put("updated_at", record.getUpdatedAt());
        if (record.getUpdatedBy() != null) {
            body.put("updated_by", record.getUpdatedBy());
        }
        return body;
    }

    private DynamicRecord fromColumnMap(Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        DynamicRecord record = new DynamicRecord(entity);
        record.setId(stringValue(row.get("id")));
        record.setVersion(numberValue(row.get("version")));
        record.setDeleted((Boolean) row.get("deleted"));
        record.setCreatedBy(stringValue(row.get("created_by")));
        record.setCreatedAt(instantValue(row.get("created_at")));
        record.setUpdatedBy(stringValue(row.get("updated_by")));
        record.setUpdatedAt(instantValue(row.get("updated_at")));
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
