package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;

import java.util.List;
import java.util.Map;

final class RecordReadProjectionPostProcessor {
    private RecordReadProjectionPostProcessor() {
    }

    static boolean supportsSqlOutput(RecordReadProjection projection) {
        if (projection == null || projection.postReadTransforms().isEmpty()) {
            return true;
        }
        return projection.postReadTransforms().stream()
                .map(RecordReadPostTransform::parse)
                .allMatch(transform -> transform
                        .map(item -> item.isFieldProtection() || item.isOptionTitle())
                        .orElse(false));
    }

    static boolean hasStorageProtectedOutput(List<StaticModuleDefinition> definitions,
                                             StaticModuleDefinition definition,
                                             RecordReadProjection projection) {
        return FieldProtectionProjectionPostProcessor.hasStorageProtectedOutput(definitions, definition, projection);
    }

    static List<Map<String, Object>> applySqlOutput(List<StaticModuleDefinition> definitions,
                                                    StaticModuleDefinition definition,
                                                    RecordReadProjection projection,
                                                    List<Map<String, Object>> records,
                                                    FieldOutputContext context) {
        return FieldProtectionProjectionPostProcessor.applySqlOutput(
                definitions,
                definition,
                projection,
                records,
                context
        );
    }

    static List<Map<String, Object>> applyStaticOutput(Class<?> modelClass,
                                                       RecordReadProjection projection,
                                                       List<Map<String, Object>> records,
                                                       OptionSourceRegistry optionSourceRegistry) {
        return OptionTitleProjectionPostProcessor.apply(modelClass, projection, records, optionSourceRegistry);
    }
}
