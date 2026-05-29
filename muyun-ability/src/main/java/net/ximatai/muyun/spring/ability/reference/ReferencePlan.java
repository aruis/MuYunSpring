package net.ximatai.muyun.spring.ability.reference;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

public record ReferencePlan(
        String sourceField,
        ReferenceTarget target,
        ReferenceCardinality cardinality,
        boolean autoTitle,
        String titleOutputField,
        List<ReferenceProjection> projections
) {
    public ReferencePlan {
        if (sourceField == null || sourceField.isBlank()) {
            throw new PlatformException("reference sourceField must not be blank");
        }
        if (target == null) {
            throw new PlatformException("reference target must not be null");
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
        if (!autoTitle && !titleOutputField.isBlank()) {
            throw new PlatformException("reference titleOutputField requires autoTitle: " + sourceField);
        }
        projections = projections == null ? List.of() : List.copyOf(projections);
        validateOutputFields(sourceField, titleOutputField, projections);
    }

    public ReferencePlan(String sourceField,
                         ReferenceTarget target,
                         ReferenceCardinality cardinality,
                         boolean autoTitle,
                         String titleOutputField) {
        this(sourceField, target, cardinality, autoTitle, titleOutputField, List.of());
    }

    public static ReferencePlan of(String sourceField, ReferenceTarget target, ReferenceCardinality cardinality) {
        return new ReferencePlan(sourceField, target, cardinality, false, "");
    }

    public ReferencePlan withAutoTitle(String titleOutputField) {
        return new ReferencePlan(sourceField, target, cardinality, true, titleOutputField, projections);
    }

    public ReferencePlan withProjection(String targetField, String outputField) {
        return new ReferencePlan(sourceField(), target, cardinality, autoTitle, titleOutputField,
                appendProjection(new ReferenceProjection(targetField, outputField)));
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

    private List<ReferenceProjection> appendProjection(ReferenceProjection projection) {
        LinkedHashSet<ReferenceProjection> next = new LinkedHashSet<>(projections);
        next.add(projection);
        return List.copyOf(next);
    }

    private static void validateOutputFields(String sourceField,
                                             String titleOutputField,
                                             List<ReferenceProjection> projections) {
        LinkedHashSet<String> outputFields = new LinkedHashSet<>();
        if (!titleOutputField.isBlank()) {
            outputFields.add(titleOutputField);
        }
        for (ReferenceProjection projection : projections) {
            if (!outputFields.add(projection.outputField())) {
                throw new PlatformException("duplicate reference outputField: "
                        + sourceField + "." + projection.outputField());
            }
        }
    }
}
