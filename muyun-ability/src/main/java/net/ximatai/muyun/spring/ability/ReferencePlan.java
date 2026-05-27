package net.ximatai.muyun.spring.ability;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public record ReferencePlan(
        String sourceField,
        ReferenceTarget target,
        ReferenceCardinality cardinality,
        boolean autoTitle,
        String titleOutputField
) {
    public ReferencePlan {
        if (sourceField == null || sourceField.isBlank()) {
            throw new AbilityException("reference sourceField must not be blank");
        }
        if (target == null) {
            throw new AbilityException("reference target must not be null");
        }
        if (cardinality == null) {
            cardinality = ReferenceCardinality.ONE;
        }
        if (titleOutputField == null) {
            titleOutputField = "";
        }
        if (autoTitle && titleOutputField.isBlank()) {
            titleOutputField = sourceField + "Title";
        }
    }

    public static ReferencePlan of(String sourceField, ReferenceTarget target, ReferenceCardinality cardinality) {
        return new ReferencePlan(sourceField, target, cardinality, false, "");
    }

    public ReferencePlan withAutoTitle(String targetField) {
        return new ReferencePlan(sourceField, target, cardinality, true, targetField);
    }

    public List<String> normalizeValues(Object value) {
        if (value == null) {
            return List.of();
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        if (cardinality == ReferenceCardinality.MANY) {
            appendMany(values, value);
        } else {
            appendOne(values, value);
        }
        return List.copyOf(values);
    }

    private void appendMany(LinkedHashSet<String> values, Object value) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> appendOne(values, item));
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                appendOne(values, Array.get(value, i));
            }
            return;
        }
        if (value instanceof String text) {
            for (String item : text.split(",")) {
                appendOne(values, item);
            }
            return;
        }
        appendOne(values, value);
    }

    private void appendOne(LinkedHashSet<String> values, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isBlank()) {
            values.add(text);
        }
    }
}
