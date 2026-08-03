package net.ximatai.muyun.spring.platform.web;

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
        return supportsSqlOutput(RecordReadProjectionGraphAdapter.adapt(projection));
    }

    static boolean supportsSqlOutput(ProjectionGraph graph) {
        if (graph == null || graph.transforms().isEmpty()) {
            return true;
        }
        return graph.transforms().stream()
                .allMatch(transform -> transform.parsed()
                        && (transform.transform().isFieldProtection() || transform.transform().isOptionLoad()));
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
        ProjectionGraph graph = projection == null ? null : RecordReadProjectionGraphAdapter.adapt(projection);
        return OptionLoadProjectionPostProcessor.apply(modelClass, graph, records, optionSourceRegistry);
    }
}
