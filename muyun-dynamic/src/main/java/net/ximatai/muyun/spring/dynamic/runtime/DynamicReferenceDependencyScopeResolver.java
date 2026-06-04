package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopePlan;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopeRequest;
import net.ximatai.muyun.spring.common.platform.ReferenceDependencyScopeResolver;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.schema.PlatformDataScopeSchema;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DynamicReferenceDependencyScopeResolver implements ReferenceDependencyScopeResolver {
    private final DynamicRecordRuntime runtime;

    public DynamicReferenceDependencyScopeResolver(DynamicRecordRuntime runtime) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime must not be null");
    }

    @Override
    public Optional<ReferenceDependencyScopePlan> resolve(ReferenceDependencyScopeRequest request) {
        ModuleDefinition module = runtime.registry().findModule(request.moduleAlias()).orElse(null);
        if (module == null || module.mainEntityAlias() == null) {
            return Optional.empty();
        }
        String sourceField = sourceField(request.referenceFieldId(), module.mainEntityAlias());
        EntityReferenceDefinition reference = module.references().stream()
                .filter(item -> module.mainEntityAlias().equals(item.sourceEntityAlias()))
                .filter(item -> sourceField.equals(item.sourceField()))
                .findFirst()
                .orElse(null);
        if (reference == null) {
            return Optional.empty();
        }
        EntityDefinition targetEntity = runtime.registry()
                .findModule(reference.target().moduleAlias())
                .flatMap(targetModule -> targetModule.entities().stream()
                        .filter(entity -> reference.target().entityAlias().equals(entity.alias()))
                        .findFirst())
                .orElse(null);
        if (targetEntity == null) {
            return Optional.empty();
        }
        return Optional.of(new ReferenceDependencyScopePlan(
                sourceField,
                reference.target().moduleAlias(),
                reference.target().entityAlias(),
                targetEntity.schemaName(),
                targetEntity.tableName(),
                fieldToColumn(targetEntity),
                runtime.operations().getDBInfo().getDatabaseType()
        ));
    }

    private String sourceField(String referenceFieldId, String mainEntityAlias) {
        String value = requireText(referenceFieldId, "referenceFieldId");
        int separatorIndex = value.lastIndexOf('.');
        if (separatorIndex < 0) {
            return value;
        }
        String entityAlias = value.substring(0, separatorIndex);
        String fieldName = value.substring(separatorIndex + 1);
        if (!mainEntityAlias.equals(entityAlias)) {
            return "";
        }
        return fieldName;
    }

    private Map<String, String> fieldToColumn(EntityDefinition entity) {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put(StandardEntitySchema.ID_FIELD, StandardEntitySchema.ID_COLUMN);
        values.put(StandardEntitySchema.ID_COLUMN, StandardEntitySchema.ID_COLUMN);
        values.put(StandardEntitySchema.TENANT_ID_FIELD, StandardEntitySchema.TENANT_ID_COLUMN);
        values.put(StandardEntitySchema.TENANT_ID_COLUMN, StandardEntitySchema.TENANT_ID_COLUMN);
        values.put(StandardEntitySchema.DELETED_FIELD, StandardEntitySchema.DELETED_COLUMN);
        values.put(StandardEntitySchema.DELETED_COLUMN, StandardEntitySchema.DELETED_COLUMN);
        if (entity.supports(EntityCapability.DATA_SCOPE)) {
            PlatformDataScopeSchema.fieldToColumn().forEach((field, column) -> {
                values.put(field, column);
                values.put(column, column);
            });
        }
        for (FieldDefinition field : entity.fields()) {
            values.put(field.fieldName(), field.columnName());
            values.put(field.columnName(), field.columnName());
        }
        return Map.copyOf(values);
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
