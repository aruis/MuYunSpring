package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.formula.FormulaRuntimeReport;
import net.ximatai.muyun.spring.common.schema.PlatformAbilityFields;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldBehaviorSupport;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionValidator;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DynamicRecord implements EntityContract {
    private final EntityDefinition entity;
    private final Map<String, FieldDefinition> fields;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final Map<String, Object> loadedValues = new LinkedHashMap<>();
    private final Map<String, List<DynamicRecord>> children = new LinkedHashMap<>();
    private final Set<String> explicitFields = new HashSet<>();
    private FormulaRuntimeReport formulaReport = new FormulaRuntimeReport();

    private String id;
    private String tenantId;
    private Integer version;
    private Boolean deleted;
    private Instant deletedAt;
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
        explicitFields.add(fieldCode);
        return this;
    }

    public Object getValue(String fieldCode) {
        if (!fields.containsKey(fieldCode)) {
            if (loadedValues.containsKey(fieldCode)) {
                return loadedValues.get(fieldCode);
            }
            throw new IllegalArgumentException("unknown dynamic field: " + fieldCode);
        }
        return values.get(fieldCode);
    }

    public Map<String, Object> getValues() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public boolean isExplicitlySet(String fieldCode) {
        return explicitFields.contains(fieldCode);
    }

    public Set<String> explicitFieldCodes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(explicitFields));
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

    public DynamicRecord copy() {
        DynamicRecord copy = new DynamicRecord(entity);
        values.forEach((fieldCode, value) -> copy.values.put(fieldCode, copyValue(value)));
        loadedValues.forEach((fieldCode, value) -> copy.loadedValues.put(fieldCode, copyValue(value)));
        copy.explicitFields.addAll(explicitFields);
        children.forEach((relationCode, records) -> copy.children.put(
                relationCode,
                records == null ? null : records.stream().map(DynamicRecord::copy).toList()
        ));
        copy.id = id;
        copy.tenantId = tenantId;
        copy.version = version;
        copy.deleted = deleted;
        copy.deletedAt = deletedAt;
        copy.createdBy = createdBy;
        copy.createdAt = createdAt;
        copy.updatedBy = updatedBy;
        copy.updatedAt = updatedAt;
        return copy;
    }

    @SuppressWarnings("unchecked")
    private Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copied = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> copied.put(key, copyValue(nestedValue)));
            return copied;
        }
        if (value instanceof List<?> list) {
            List<Object> copied = new ArrayList<>();
            list.forEach(item -> copied.add(copyValue(item)));
            return copied;
        }
        if (value instanceof Set<?> set) {
            Set<Object> copied = new LinkedHashSet<>();
            set.forEach(item -> copied.add(copyValue(item)));
            return copied;
        }
        if (value instanceof byte[] bytes) {
            return bytes.clone();
        }
        if (value instanceof Object[] objects) {
            Object[] copied = objects.clone();
            for (int i = 0; i < copied.length; i++) {
                copied[i] = copyValue(copied[i]);
            }
            return copied;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return new java.sql.Timestamp(timestamp.getTime());
        }
        if (value instanceof java.sql.Date date) {
            return new java.sql.Date(date.getTime());
        }
        if (value instanceof Date date) {
            return new Date(date.getTime());
        }
        return value;
    }

    String parentId() {
        if (!entity.supports(EntityCapability.TREE) || !hasField(PlatformAbilityFields.TREE_PARENT_FIELD)) {
            return null;
        }
        return stringValue(values.get(PlatformAbilityFields.TREE_PARENT_FIELD));
    }

    void parentId(String parentId) {
        if (entity.supports(EntityCapability.TREE)) {
            setPlatformValueIfPresent(PlatformAbilityFields.TREE_PARENT_FIELD, parentId);
        }
    }

    Integer sortOrder() {
        if (!entity.supports(EntityCapability.SORT) || !hasField(PlatformAbilityFields.SORT_FIELD)) {
            return null;
        }
        return numberValue(values.get(PlatformAbilityFields.SORT_FIELD));
    }

    void sortOrder(Integer sortOrder) {
        if (entity.supports(EntityCapability.SORT)) {
            setPlatformValueIfPresent(PlatformAbilityFields.SORT_FIELD, sortOrder);
        }
    }

    String title() {
        if (!entity.supports(EntityCapability.REFERENCE) || !hasField(PlatformAbilityFields.TITLE_FIELD)) {
            return null;
        }
        return stringValue(values.get(PlatformAbilityFields.TITLE_FIELD));
    }

    Boolean enabled() {
        if (!hasField(PlatformAbilityFields.ENABLED_FIELD)) {
            return null;
        }
        Object value = values.get(PlatformAbilityFields.ENABLED_FIELD);
        return value instanceof Boolean enabled ? enabled : null;
    }

    void enabled(Boolean enabled) {
        setPlatformValueIfPresent(PlatformAbilityFields.ENABLED_FIELD, enabled);
    }

    Set<String> fieldCodes() {
        return fields.keySet();
    }

    void putLoadedValue(String fieldCode, Object value) {
        if (fields.containsKey(fieldCode)) {
            values.put(fieldCode, value);
        } else {
            loadedValues.put(fieldCode, value);
        }
    }

    void putVirtualValue(String fieldCode, Object value) {
        loadedValues.put(fieldCode, value);
    }

    void putPlatformValue(String fieldCode, Object value) {
        FieldDefinition field = fields.get(fieldCode);
        if (field == null) {
            throw new IllegalArgumentException("unknown dynamic field: " + fieldCode);
        }
        validateValue(field, value);
        values.put(fieldCode, value);
    }

    void validateForInsert() {
        for (FieldDefinition field : fields.values()) {
            if (field.isRequired() && values.get(field.code()) == null) {
                throw new IllegalArgumentException("required dynamic field is missing: " + field.code());
            }
        }
    }

    public FormulaRuntimeReport formulaReport() {
        return formulaReport;
    }

    void formulaReport(FormulaRuntimeReport formulaReport) {
        this.formulaReport = formulaReport == null ? new FormulaRuntimeReport() : formulaReport;
    }

    void applyDefaultsForInsert() {
        for (FieldDefinition field : fields.values()) {
            if (!values.containsKey(field.code()) && field.behavior().defaultValue() != null) {
                Object defaultValue = FieldBehaviorSupport.parseDefaultValue(field.type(), field.behavior().defaultValue());
                validateValue(field, defaultValue);
                values.put(field.code(), defaultValue);
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
        if (field.behavior().validationRegex() != null
                && value instanceof String text
                && !text.matches(field.behavior().validationRegex())) {
            throw new IllegalArgumentException("dynamic field value does not match validationRegex: " + field.code());
        }
    }

    private boolean hasField(String fieldCode) {
        return fields.containsKey(fieldCode);
    }

    private void setPlatformValueIfPresent(String fieldCode, Object value) {
        if (hasField(fieldCode)) {
            putPlatformValue(fieldCode, value);
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
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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
    public Instant getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
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
