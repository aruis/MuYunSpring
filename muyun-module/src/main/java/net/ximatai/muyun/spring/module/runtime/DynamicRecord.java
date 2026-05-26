package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.common.model.EntityContract;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldType;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicRecord implements EntityContract {
    private final EntityDefinition entity;
    private final Map<String, FieldDefinition> fields;
    private final Map<String, Object> values = new LinkedHashMap<>();

    private String id;
    private Integer version;
    private Boolean deleted;
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;

    public DynamicRecord(EntityDefinition entity) {
        new ModuleDefinitionValidator().validateEntity(entity);
        this.entity = entity;
        this.fields = entity.fields().stream()
                .collect(Collectors.toUnmodifiableMap(FieldDefinition::code, Function.identity()));
    }

    public EntityDefinition getEntity() {
        return entity;
    }

    public DynamicRecord setValue(String fieldCode, Object value) {
        FieldDefinition field = fields.get(fieldCode);
        if (field == null) {
            throw new IllegalArgumentException("unknown dynamic field: " + fieldCode);
        }
        validateValue(field, value);
        values.put(fieldCode, value);
        return this;
    }

    public Object getValue(String fieldCode) {
        if (!fields.containsKey(fieldCode)) {
            throw new IllegalArgumentException("unknown dynamic field: " + fieldCode);
        }
        return values.get(fieldCode);
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    Set<String> fieldCodes() {
        return fields.keySet();
    }

    void putLoadedValue(String fieldCode, Object value) {
        if (fields.containsKey(fieldCode)) {
            values.put(fieldCode, value);
        }
    }

    void validateForInsert() {
        for (FieldDefinition field : fields.values()) {
            if (field.isRequired() && values.get(field.code()) == null) {
                throw new IllegalArgumentException("required dynamic field is missing: " + field.code());
            }
        }
    }

    private void validateValue(FieldDefinition field, Object value) {
        if (value == null) {
            if (field.isRequired()) {
                throw new IllegalArgumentException("required dynamic field must not be null: " + field.code());
            }
            return;
        }
        FieldType type = field.type();
        boolean matched = switch (type) {
            case STRING, TEXT -> value instanceof String;
            case INTEGER -> value instanceof Integer;
            case LONG -> value instanceof Long;
            case BOOLEAN -> value instanceof Boolean;
            case TIMESTAMP -> value instanceof Instant || value instanceof java.sql.Timestamp;
            case DATE -> value instanceof LocalDate || value instanceof java.sql.Date;
            case DECIMAL -> value instanceof BigDecimal || value instanceof Number;
            case JSON -> true;
        };
        if (!matched) {
            throw new IllegalArgumentException("invalid value type for dynamic field: " + field.code());
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public Boolean getDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String getUpdatedBy() {
        return updatedBy;
    }

    @Override
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
