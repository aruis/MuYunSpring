package net.ximatai.muyun.spring.platform.metadata;

import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitConversionMode;
import net.ximatai.muyun.spring.dynamic.metadata.FieldMeasureUnitMode;

public record ModuleMetadataMeasureUnitPrepareCommand(
        String unitCategoryAlias,
        FieldMeasureUnitMode unitMode,
        String fixedUnitCode,
        String defaultUnitCode,
        String unitFieldName,
        String baseValueFieldName,
        String baseUnitCategoryAlias,
        String baseUnitCode,
        FieldMeasureUnitConversionMode unitConversionMode,
        String conversionScopeFieldId,
        Boolean unitRequired,
        String unitFieldTypeAlias,
        String baseValueFieldTypeAlias
) {
}
