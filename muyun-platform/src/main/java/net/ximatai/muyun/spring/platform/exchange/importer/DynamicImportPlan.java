package net.ximatai.muyun.spring.platform.exchange.importer;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;
import net.ximatai.muyun.spring.dynamic.descriptor.DynamicReferenceDescriptor;

import java.util.List;

public record DynamicImportPlan(
        String moduleAlias,
        String planSource,
        List<SheetPlan> sheets
) {
    public DynamicImportPlan {
        requireText(moduleAlias, "moduleAlias must not be blank");
        if (sheets == null || sheets.isEmpty()) {
            throw new PlatformException("dynamic import plan sheets must not be empty");
        }
        sheets = List.copyOf(sheets);
    }

    public SheetPlan mainSheet() {
        return sheets.stream()
                .filter(SheetPlan::main)
                .findFirst()
                .orElseThrow(() -> new PlatformException("dynamic import plan main sheet not found"));
    }

    public record SheetPlan(
            String sheetKey,
            String entityAlias,
            String sheetName,
            boolean main,
            String matchFieldName,
            ImportDuplicateStrategy duplicateStrategy,
            List<FieldPlan> fields
    ) {
        public SheetPlan {
            requireText(sheetKey, "sheetKey must not be blank");
            requireText(entityAlias, "entityAlias must not be blank");
            requireText(sheetName, "sheetName must not be blank");
            requireText(matchFieldName, "matchFieldName must not be blank");
            if (duplicateStrategy == null) {
                throw new PlatformException("duplicateStrategy must not be null: " + sheetKey);
            }
            if (fields == null || fields.isEmpty()) {
                throw new PlatformException("sheet fields must not be empty: " + sheetKey);
            }
            fields = List.copyOf(fields);
        }
    }

    public record FieldPlan(
            String entityAlias,
            String fieldName,
            String title,
            FieldType fieldType,
            boolean relateId,
            boolean matchKeyCandidate,
            boolean companion,
            DynamicReferenceDescriptor reference
    ) {
        public FieldPlan(String entityAlias,
                         String fieldName,
                         String title,
                         boolean relateId,
                         boolean matchKeyCandidate,
                         boolean companion) {
            this(entityAlias, fieldName, title, relateId ? FieldType.TEXT : FieldType.STRING,
                    relateId, matchKeyCandidate, companion, null);
        }

        public FieldPlan(String entityAlias,
                         String fieldName,
                         String title,
                         FieldType fieldType,
                         boolean relateId,
                         boolean matchKeyCandidate,
                         boolean companion) {
            this(entityAlias, fieldName, title, fieldType, relateId, matchKeyCandidate, companion, null);
        }

        public FieldPlan {
            requireText(entityAlias, "field entityAlias must not be blank");
            requireText(fieldName, "fieldName must not be blank");
            requireText(title, "field title must not be blank");
            if (fieldType == null) {
                throw new PlatformException("fieldType must not be null: " + entityAlias + "." + fieldName);
            }
            if (relateId && fieldType != FieldType.TEXT && fieldType != FieldType.STRING) {
                throw new PlatformException("relateId field type must be text: " + fieldName);
            }
            if (relateId && (matchKeyCandidate || companion)) {
                throw new PlatformException("relateId field cannot be match candidate or companion");
            }
            if (companion && matchKeyCandidate) {
                throw new PlatformException("companion field cannot be match candidate: " + fieldName);
            }
        }
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new PlatformException(message);
        }
    }
}
