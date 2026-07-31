package net.ximatai.muyun.spring.dynamic.web;

import net.ximatai.muyun.spring.web.WebSort;
import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.common.schema.PlatformDataScopeSchema;
import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicAbilityFields;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicEntityOperations;
import net.ximatai.muyun.spring.platform.metadata.MetadataFieldForm;
import net.ximatai.muyun.spring.platform.metadata.RelationRole;
import net.ximatai.muyun.spring.platform.metadata.ResolvedModuleMetadataField;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.LinkedHashSet;

final class DynamicWebQueryFieldSupport {
    private static final Set<String> SEARCHABLE_TEXT_TYPES = Set.of("string", "text");

    private DynamicWebQueryFieldSupport() {
    }

    static void validatePhysicalSorts(DynamicEntityOperations operations, List<WebSort> sorts) {
        validatePhysicalSorts(operations, sorts, Set.of());
    }

    static void validatePhysicalSorts(DynamicEntityOperations operations,
                                      List<WebSort> sorts,
                                      Set<String> additionalSortableFields) {
        if (sorts == null || sorts.isEmpty()) {
            return;
        }
        Set<String> fields = queryableSqlFields(operations.newRecord().getEntity());
        if (additionalSortableFields != null) {
            fields.addAll(additionalSortableFields);
        }
        for (WebSort sort : sorts) {
            String fieldName = sort == null ? null : trim(sort.field());
            if (fieldName == null || !fields.contains(fieldName)) {
                throw new PlatformException("Sort field is not a physical dynamic field: " + fieldName);
            }
        }
    }

    static boolean searchableTextField(ResolvedModuleMetadataField field) {
        if (field.relationRole() != RelationRole.MAIN || field.fieldForm() == MetadataFieldForm.VIRTUAL) {
            return false;
        }
        String alias = field.fieldTypeAlias();
        return alias != null && SEARCHABLE_TEXT_TYPES.contains(alias.trim().toLowerCase(Locale.ROOT));
    }

    private static Set<String> queryableSqlFields(EntityDefinition entity) {
        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(StandardEntitySchema.fieldNames());
        fields.addAll(StandardEntitySchema.columnNames());
        if (entity.supports(EntityCapability.DATA_SCOPE)) {
            PlatformDataScopeSchema.fieldToColumn().forEach((field, column) -> {
                fields.add(field);
                fields.add(column);
            });
        }
        if (entity.supports(EntityCapability.APPROVAL)) {
            addFields(fields, DynamicAbilityFields.approvalFields());
        }
        addFields(fields, entity.fields().stream()
                .filter(FieldDefinition::isPhysical)
                .toList());
        return fields;
    }

    private static void addFields(Set<String> fields, List<FieldDefinition> definitions) {
        definitions.forEach(field -> {
            fields.add(field.fieldName());
            fields.add(field.columnName());
        });
    }

    private static String trim(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isBlank() ? null : text;
    }
}
