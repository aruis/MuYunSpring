package net.ximatai.muyun.spring.platform.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class LowCodeMetadataFieldProjection {
    private final Map<String, Object> field;
    private final Map<String, String> fieldNameById;

    private LowCodeMetadataFieldProjection(Map<String, Object> field, Map<String, String> fieldNameById) {
        this.field = field;
        this.fieldNameById = fieldNameById;
    }

    static List<LowCodeMetadataFieldProjection> from(LowCodeConfigBundle bundle) {
        if (bundle == null || bundle.content().isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> fields = fields(bundle.content());
        Map<String, String> fieldNameById = fields.stream()
                .filter(field -> text(field, "id") != null || text(field, "metadataFieldId") != null)
                .filter(field -> text(field, "fieldName") != null)
                .collect(Collectors.toMap(
                        LowCodeMetadataFieldProjection::fieldIdentity,
                        field -> text(field, "fieldName"),
                        (left, right) -> left
                ));
        return fields.stream()
                .map(field -> new LowCodeMetadataFieldProjection(field, fieldNameById))
                .toList();
    }

    String fieldName() {
        return firstNonBlank(text("fieldName"), fieldNameById.get(ownerFieldId()));
    }

    String ownerFieldId() {
        return firstText("metadataFieldId", "id");
    }

    String runtimeFieldType() {
        String type = firstText("fieldType", "type");
        return type == null ? null : type.trim().toUpperCase();
    }

    String text(String key) {
        return text(field, key);
    }

    Map<String, Object> nested(String key) {
        Object value = field.get(key);
        if (value instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        return Map.of();
    }

    String firstText(String... keys) {
        for (String key : keys) {
            String value = text(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    String firstText(Map<String, Object> nested, String... keys) {
        for (String key : keys) {
            String value = text(key);
            if (value != null) {
                return value;
            }
            value = text(nested, key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    String relatedFieldName(Map<String, Object> nested,
                            String nameKey,
                            String nestedNameKey,
                            String idKey,
                            String nestedIdKey) {
        String fieldName = firstText(nested, nameKey, nestedNameKey);
        if (fieldName != null) {
            return fieldName;
        }
        String fieldId = firstText(nested, idKey, nestedIdKey);
        return fieldId == null ? null : fieldNameById.get(fieldId);
    }

    String relatedFieldName(Map<String, Object> nested, String nameKey, String idKey) {
        String fieldName = firstText(nested, nameKey);
        if (fieldName != null) {
            return fieldName;
        }
        String fieldId = firstText(nested, idKey);
        return fieldId == null ? null : fieldNameById.get(fieldId);
    }

    private static List<Map<String, Object>> fields(Map<String, Object> content) {
        List<Map<String, Object>> fields = new ArrayList<>();
        addFields(fields, content.get("fields"));
        addFields(fields, content.get("metadataFields"));
        addFields(fields, content.get("moduleFields"));
        return List.copyOf(fields);
    }

    private static void addFields(List<Map<String, Object>> fields, Object value) {
        if (!(value instanceof List<?> list)) {
            return;
        }
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                fields.add(normalizeMap(map));
            }
        }
    }

    private static String fieldIdentity(Map<String, Object> field) {
        String id = text(field, "id");
        return id == null ? text(field, "metadataFieldId") : id;
    }

    private static Map<String, Object> normalizeMap(Map<?, ?> map) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() instanceof String key) {
                normalized.put(key, entry.getValue());
            }
        }
        return normalized;
    }

    private static String text(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private static String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }
}
