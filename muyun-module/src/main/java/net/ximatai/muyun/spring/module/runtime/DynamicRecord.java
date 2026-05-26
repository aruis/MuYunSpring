package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.common.model.EntityContract;
import net.ximatai.muyun.spring.common.model.EnabledCapable;
import net.ximatai.muyun.spring.common.model.TitledCapable;
import net.ximatai.muyun.spring.common.model.TreeCapable;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.EntityCapability;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldType;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionValidator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicRecord implements EntityContract, TreeCapable, TitledCapable, EnabledCapable {
    private final EntityDefinition entity;
    private final Map<String, FieldDefinition> fields;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, List<DynamicRecord>> children = new LinkedHashMap<>();

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

    public DynamicRecord setChildren(String relationCode, List<DynamicRecord> records) {
        if (relationCode == null || relationCode.isBlank()) {
            throw new IllegalArgumentException("relationCode must not be blank");
        }
        if (records == null) {
            children.put(relationCode, null);
            return this;
        }
        children.put(relationCode, List.copyOf(records));
        return this;
    }

    public List<DynamicRecord> getChildren(String relationCode) {
        return children.get(relationCode);
    }

    public Map<String, List<DynamicRecord>> getChildren() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(children));
    }

    @Override
    public String getParentId() {
        if (!entity.supports(EntityCapability.TREE) || !hasField(PlatformAbilityFields.TREE_PARENT_FIELD)) {
            return null;
        }
        return stringValue(values.get(PlatformAbilityFields.TREE_PARENT_FIELD));
    }

    @Override
    public void setParentId(String parentId) {
        if (entity.supports(EntityCapability.TREE)) {
            setAbilityValueIfPresent(PlatformAbilityFields.TREE_PARENT_FIELD, parentId);
        }
    }

    @Override
    public Integer getSortOrder() {
        if (!entity.supports(EntityCapability.SORT) || !hasField(PlatformAbilityFields.SORT_FIELD)) {
            return null;
        }
        return numberValue(values.get(PlatformAbilityFields.SORT_FIELD));
    }

    @Override
    public void setSortOrder(Integer sortOrder) {
        if (entity.supports(EntityCapability.SORT)) {
            setAbilityValueIfPresent(PlatformAbilityFields.SORT_FIELD, sortOrder);
        }
    }

    @Override
    public String getTitle() {
        if (!entity.supports(EntityCapability.REFERENCE) || !hasField(PlatformAbilityFields.TITLE_FIELD)) {
            return null;
        }
        return stringValue(values.get(PlatformAbilityFields.TITLE_FIELD));
    }

    @Override
    public Boolean getEnabled() {
        if (!hasField(PlatformAbilityFields.ENABLED_FIELD)) {
            return null;
        }
        Object value = values.get(PlatformAbilityFields.ENABLED_FIELD);
        return value instanceof Boolean enabled ? enabled : null;
    }

    @Override
    public void setEnabled(Boolean enabled) {
        setAbilityValueIfPresent(PlatformAbilityFields.ENABLED_FIELD, enabled);
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

    private boolean hasField(String fieldCode) {
        return fields.containsKey(fieldCode);
    }

    private void setAbilityValueIfPresent(String fieldCode, Object value) {
        if (hasField(fieldCode)) {
            setValue(fieldCode, value);
        }
    }

    private Integer numberValue(Object value) {
        return value instanceof Number number ? number.intValue() : null;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
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
