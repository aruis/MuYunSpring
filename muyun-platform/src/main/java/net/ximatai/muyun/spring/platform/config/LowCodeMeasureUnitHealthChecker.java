package net.ximatai.muyun.spring.platform.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LowCodeMeasureUnitHealthChecker implements LowCodeModuleHealthChecker {
    @Override
    public List<LowCodeConfigHealthItem> check(LowCodeModuleHealthContext context) {
        LowCodeModulePackage modulePackage = context == null ? null : context.modulePackage();
        if (modulePackage == null || !modulePackage.includes(LowCodePackageBundleType.METADATA)) {
            return List.of();
        }
        LowCodeConfigBundle bundle = modulePackage.bundleMap().get(LowCodePackageBundleType.METADATA);
        if (bundle == null || bundle.content().isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rawFields = fields(bundle.content());
        List<FieldContract> fields = contracts(rawFields);
        if (fields.isEmpty()) {
            return List.of();
        }
        Set<String> fieldNames = fields.stream()
                .map(FieldContract::fieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> measureDependencies = modulePackage.dependencyManifest().dependencies().stream()
                .filter(dependency -> dependency != null && dependency.type() == LowCodePackageDependencyType.MEASURE_UNIT)
                .map(dependency -> dependency.applicationAlias() + ":" + dependency.alias())
                .collect(Collectors.toSet());
        List<LowCodeConfigHealthItem> items = new ArrayList<>();
        for (FieldContract field : fields) {
            if (field.unitCategoryAlias() == null) {
                continue;
            }
            String targetId = field.fieldName() == null ? field.unitCategoryAlias() : field.fieldName();
            requireDependency(items, modulePackage.applicationAlias(), field.unitCategoryAlias(), measureDependencies, targetId);
            String baseUnitCategoryAlias = field.baseUnitCategoryAlias() == null
                    ? field.unitCategoryAlias()
                    : field.baseUnitCategoryAlias();
            if (!Objects.equals(field.unitCategoryAlias(), baseUnitCategoryAlias)) {
                requireDependency(items, modulePackage.applicationAlias(), baseUnitCategoryAlias, measureDependencies, targetId);
            }
            requireText(items, field.baseUnitCode(), "MEASURE_UNIT_BASE_UNIT_MISSING",
                    "measure unit field requires baseUnitCode", targetId);
            requireBaseValueField(items, field, fieldNames, targetId);
            if (field.unitMode() == null) {
                items.add(error("MEASURE_UNIT_MODE_MISSING", "measure unit field requires unitMode", targetId,
                        "Set unitMode to FIXED or SELECTABLE"));
            } else if ("FIXED".equals(field.unitMode())) {
                requireText(items, field.fixedUnitCode(), "MEASURE_UNIT_FIXED_UNIT_MISSING",
                        "fixed measure unit field requires fixedUnitCode", targetId);
            } else if ("SELECTABLE".equals(field.unitMode())) {
                requireRelatedField(items, field.unitFieldName(), fieldNames,
                        "MEASURE_UNIT_COMPANION_MISSING", "selectable measure unit field requires unit companion field",
                        targetId);
            }
            requireOptionalRelatedField(items, field.conversionScopeFieldName(), fieldNames,
                    "MEASURE_UNIT_SCOPE_FIELD_MISSING", "measure unit conversion scope field is missing", targetId);
        }
        return List.copyOf(items);
    }

    private void requireDependency(List<LowCodeConfigHealthItem> items,
                                   String applicationAlias,
                                   String categoryAlias,
                                   Set<String> dependencies,
                                   String targetId) {
        String dependencyKey = applicationAlias + ":" + categoryAlias;
        if (dependencies.contains(dependencyKey)) {
            return;
        }
        items.add(LowCodeConfigHealthItem.warn(
                LowCodeConfigHealthScope.DEPENDENCY,
                "MEASURE_UNIT_DEPENDENCY_MISSING",
                "measure unit category is not declared in dependency manifest",
                "measureUnit",
                targetId,
                "Declare MEASURE_UNIT dependency " + dependencyKey + " for cross-environment migration"
        ));
    }

    private void requireBaseValueField(List<LowCodeConfigHealthItem> items,
                                       FieldContract field,
                                       Set<String> fieldNames,
                                       String targetId) {
        String baseValueFieldName = field.baseValueFieldName();
        String ownerFieldName = field.fieldName();
        if (baseValueFieldName == null) {
            items.add(error("MEASURE_UNIT_BASE_VALUE_MISSING",
                    "measure unit field requires base value field", targetId,
                    "Add a shadow base value field and bind baseValueFieldName"));
            return;
        }
        if (Objects.equals(ownerFieldName, baseValueFieldName)
                || (field.ownerFieldId() != null && field.baseValueFieldId() != null
                && Objects.equals(field.ownerFieldId(), field.baseValueFieldId()))) {
            items.add(error("MEASURE_UNIT_BASE_VALUE_CONFLICT",
                    "measure base value field must be different from owner", targetId,
                    "Use a dedicated shadow field such as " + ownerFieldName + "Base"));
            return;
        }
        if (!fieldNames.contains(baseValueFieldName)) {
            items.add(error("MEASURE_UNIT_BASE_VALUE_MISSING",
                    "measure base value field is missing from metadata fields", targetId,
                    "Include the shadow base value field in the metadata bundle"));
        }
    }

    private void requireRelatedField(List<LowCodeConfigHealthItem> items,
                                     String fieldName,
                                     Set<String> fieldNames,
                                     String code,
                                     String message,
                                     String targetId) {
        if (fieldName == null || !fieldNames.contains(fieldName)) {
            items.add(error(code, message, targetId, "Include and bind related field"));
        }
    }

    private void requireOptionalRelatedField(List<LowCodeConfigHealthItem> items,
                                             String fieldName,
                                             Set<String> fieldNames,
                                             String code,
                                             String message,
                                             String targetId) {
        if (fieldName != null && !fieldNames.contains(fieldName)) {
            items.add(error(code, message, targetId, "Include or remove related field"));
        }
    }

    private void requireText(List<LowCodeConfigHealthItem> items,
                             String value,
                             String code,
                             String message,
                             String targetId) {
        if (value == null) {
            items.add(error(code, message, targetId, "Set required measure unit field"));
        }
    }

    private LowCodeConfigHealthItem error(String code, String message, String targetId, String suggestion) {
        return LowCodeConfigHealthItem.error(
                LowCodeConfigHealthScope.METADATA,
                code,
                message,
                "field",
                targetId,
                suggestion
        );
    }

    private List<Map<String, Object>> fields(Map<String, Object> content) {
        List<Map<String, Object>> fields = new ArrayList<>();
        addFields(fields, content.get("fields"));
        addFields(fields, content.get("metadataFields"));
        addFields(fields, content.get("moduleFields"));
        return List.copyOf(fields);
    }

    private List<FieldContract> contracts(List<Map<String, Object>> rawFields) {
        Map<String, String> fieldNameById = rawFields.stream()
                .filter(field -> text(field, "id") != null || text(field, "metadataFieldId") != null)
                .filter(field -> text(field, "fieldName") != null)
                .collect(Collectors.toMap(
                        this::fieldIdentity,
                        field -> text(field, "fieldName"),
                        (left, right) -> left
                ));
        return rawFields.stream()
                .map(field -> contract(field, fieldNameById))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private FieldContract contract(Map<String, Object> field, Map<String, String> fieldNameById) {
        Map<String, Object> measureUnit = field.get("measureUnit") instanceof Map<?, ?> map
                ? normalizeMap(map)
                : Map.of();
        String ownerFieldId = firstText(field, measureUnit, "metadataFieldId", "id");
        return new FieldContract(
                firstNonBlank(text(field, "fieldName"), fieldNameById.get(ownerFieldId)),
                ownerFieldId,
                firstText(field, measureUnit, "unitCategoryAlias", "categoryAlias"),
                firstText(field, measureUnit, "unitMode", "mode"),
                firstText(field, measureUnit, "fixedUnitCode"),
                firstText(field, measureUnit, "baseUnitCategoryAlias"),
                firstText(field, measureUnit, "baseUnitCode"),
                relatedFieldName(field, measureUnit, fieldNameById, "unitFieldName", "unitFieldId"),
                relatedFieldName(field, measureUnit, fieldNameById, "baseValueFieldName", "baseValueFieldId"),
                firstText(field, measureUnit, "baseValueFieldId"),
                relatedFieldName(field, measureUnit, fieldNameById, "conversionScopeFieldName", "conversionScopeFieldId")
        );
    }

    private String fieldIdentity(Map<String, Object> field) {
        String id = text(field, "id");
        return id == null ? text(field, "metadataFieldId") : id;
    }

    private String relatedFieldName(Map<String, Object> field,
                                    Map<String, Object> measureUnit,
                                    Map<String, String> fieldNameById,
                                    String nameKey,
                                    String idKey) {
        String fieldName = firstText(field, measureUnit, nameKey);
        if (fieldName != null) {
            return fieldName;
        }
        String fieldId = firstText(field, measureUnit, idKey);
        return fieldId == null ? null : fieldNameById.get(fieldId);
    }

    private String firstText(Map<String, Object> field, Map<String, Object> measureUnit, String... keys) {
        for (String key : keys) {
            String value = text(field, key);
            if (value != null) {
                return value;
            }
            value = text(measureUnit, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    private void addFields(List<Map<String, Object>> fields, Object value) {
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                fields.add(normalizeMap(map));
            }
        }
    }

    private Map<String, Object> normalizeMap(Map<?, ?> map) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                normalized.put(key, entry.getValue());
            }
        }
        return normalized;
    }

    private String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private record FieldContract(
            String fieldName,
            String ownerFieldId,
            String unitCategoryAlias,
            String unitMode,
            String fixedUnitCode,
            String baseUnitCategoryAlias,
            String baseUnitCode,
            String unitFieldName,
            String baseValueFieldName,
            String baseValueFieldId,
            String conversionScopeFieldName
    ) {
    }
}
