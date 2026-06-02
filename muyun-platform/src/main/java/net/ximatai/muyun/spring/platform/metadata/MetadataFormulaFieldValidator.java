package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.PageRequest;
import net.ximatai.muyun.spring.common.exception.PlatformException;

import java.util.Set;

final class MetadataFormulaFieldValidator {
    private final ModuleMetadataRelationService relationService;
    private final MetadataFieldService fieldService;

    MetadataFormulaFieldValidator(ModuleMetadataRelationService relationService, MetadataFieldService fieldService) {
        this.relationService = relationService;
        this.fieldService = fieldService;
    }

    void validateExpressionFields(Set<String> fieldPaths, ModuleMetadataRelation relation, String context) {
        for (String fieldPath : fieldPaths) {
            validateFieldPath(fieldPath, relation, context + " field");
        }
    }

    void validateTargetField(String targetField, ModuleMetadataRelation relation, String context) {
        if (targetField == null || targetField.isBlank()) {
            return;
        }
        validateFieldPath(targetField, relation, context + " target field");
    }

    private void validateFieldPath(String fieldPath, ModuleMetadataRelation relation, String context) {
        if (!fieldPath.contains(".")) {
            requireMetadataField(relation.getMetadataId(), fieldPath, context);
            return;
        }
        String[] parts = fieldPath.split("\\.");
        if (parts.length != 2) {
            throw new PlatformException(context + " is invalid: " + fieldPath);
        }
        ModuleMetadataRelation childRelation = relationService.list(Criteria.of()
                        .eq("moduleAlias", relation.getModuleAlias())
                        .eq("parentMetadataId", relation.getMetadataId())
                        .eq("relationAlias", parts[0]), new PageRequest(0, 1)).stream()
                .findFirst()
                .orElse(null);
        if (childRelation == null) {
            throw new PlatformException(context + " relation does not exist: " + parts[0]);
        }
        requireMetadataField(childRelation.getMetadataId(), parts[1], context);
    }

    private void requireMetadataField(String metadataId, String fieldName, String context) {
        if (fieldService.count(Criteria.of()
                .eq("metadataId", metadataId)
                .eq("fieldName", fieldName)) <= 0) {
            throw new PlatformException(context + " does not exist: " + fieldName);
        }
    }
}
