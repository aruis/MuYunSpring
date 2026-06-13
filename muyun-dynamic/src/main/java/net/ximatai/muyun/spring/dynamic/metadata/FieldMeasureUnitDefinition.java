package net.ximatai.muyun.spring.dynamic.metadata;

public record FieldMeasureUnitDefinition(
        String categoryAlias,
        FieldMeasureUnitMode mode,
        String fixedUnitCode,
        String defaultUnitCode,
        String unitFieldName,
        String baseValueFieldName,
        String baseUnitCode,
        FieldMeasureUnitConversionMode conversionMode,
        String conversionScopeFieldName,
        boolean unitRequired
) {
    public static final FieldMeasureUnitDefinition NONE = new FieldMeasureUnitDefinition(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            false
    );

    public boolean enabled() {
        return categoryAlias != null && !categoryAlias.isBlank();
    }
}
