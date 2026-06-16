package net.ximatai.muyun.spring.platform.config;

import net.ximatai.muyun.spring.platform.measure.MeasureUnitCategoryService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
        List<FieldContract> fields = LowCodeMetadataFieldProjection.from(bundle).stream()
                .map(this::contract)
                .toList();
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
        String sharedDependencyKey = MeasureUnitCategoryService.SHARED_APPLICATION_ALIAS + ":" + categoryAlias;
        if (dependencies.contains(sharedDependencyKey) || dependencies.contains(dependencyKey)) {
            return;
        }
        items.add(LowCodeConfigHealthItem.warn(
                LowCodeConfigHealthScope.DEPENDENCY,
                "MEASURE_UNIT_DEPENDENCY_MISSING",
                "measure unit category is not declared in dependency manifest",
                "measureUnit",
                targetId,
                "Declare MEASURE_UNIT dependency " + sharedDependencyKey + " for cross-environment migration"
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

    private FieldContract contract(LowCodeMetadataFieldProjection field) {
        Map<String, Object> measureUnit = field.nested("measureUnit");
        return new FieldContract(
                field.fieldName(),
                field.ownerFieldId(),
                field.firstText(measureUnit, "unitCategoryAlias", "categoryAlias"),
                field.firstText(measureUnit, "unitMode", "mode"),
                field.firstText(measureUnit, "fixedUnitCode"),
                field.firstText(measureUnit, "baseUnitCategoryAlias"),
                field.firstText(measureUnit, "baseUnitCode"),
                field.relatedFieldName(measureUnit, "unitFieldName", "unitFieldId"),
                field.relatedFieldName(measureUnit, "baseValueFieldName", "baseValueFieldId"),
                field.firstText(measureUnit, "baseValueFieldId"),
                field.relatedFieldName(measureUnit, "conversionScopeFieldName", "conversionScopeFieldId")
        );
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
