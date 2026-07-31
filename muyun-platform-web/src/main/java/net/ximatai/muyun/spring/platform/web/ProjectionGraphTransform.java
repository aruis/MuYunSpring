package net.ximatai.muyun.spring.platform.web;

public record ProjectionGraphTransform(String rawExpression,
                                       RecordReadPostTransform transform) {
    public ProjectionGraphTransform {
        if (rawExpression == null || rawExpression.isBlank()) {
            throw new IllegalArgumentException("projection graph transform expression must not be blank");
        }
        rawExpression = rawExpression.trim();
    }

    public boolean parsed() {
        return transform != null;
    }

    public String transformType() {
        return transform == null ? null : transform.transformType();
    }

    public String fieldName() {
        return transform == null ? null : transform.fieldName();
    }
}
