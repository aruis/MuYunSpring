package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.common.option.OptionFieldDefinition;
import net.ximatai.muyun.spring.common.option.OptionFieldResolver;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class OptionTitleProjectionPostProcessor {
    private OptionTitleProjectionPostProcessor() {
    }

    static List<Map<String, Object>> apply(Class<?> modelClass,
                                           RecordReadProjection projection,
                                           List<Map<String, Object>> records,
                                           OptionSourceRegistry optionSourceRegistry) {
        ProjectionGraph graph = projection == null ? null : RecordReadProjectionGraphAdapter.adapt(projection);
        return apply(modelClass, graph, records, optionSourceRegistry);
    }

    static List<Map<String, Object>> apply(Class<?> modelClass,
                                           ProjectionGraph graph,
                                           List<Map<String, Object>> records,
                                           OptionSourceRegistry optionSourceRegistry) {
        if (modelClass == null || graph == null || records == null || records.isEmpty()
                || optionSourceRegistry == null) {
            return records;
        }
        Set<String> optionTitleFields = graph.parsedTransforms().stream()
                .filter(RecordReadPostTransform::isOptionTitle)
                .map(RecordReadPostTransform::fieldName)
                .collect(Collectors.toUnmodifiableSet());
        if (optionTitleFields.isEmpty()) {
            return records;
        }
        List<OptionFieldDefinition> definitions = OptionFieldResolver.resolve(modelClass).stream()
                .filter(OptionFieldDefinition::hasTitleOutput)
                .filter(definition -> optionTitleFields.contains(definition.fieldName()))
                .toList();
        if (definitions.isEmpty()) {
            return records;
        }
        List<Map<String, Object>> projected = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            LinkedHashMap<String, Object> output = new LinkedHashMap<>(record);
            for (OptionFieldDefinition definition : definitions) {
                if (!output.containsKey(definition.fieldName()) || output.containsKey(definition.titleOutputField())) {
                    continue;
                }
                output.put(definition.titleOutputField(), titleValue(definition, output.get(definition.fieldName()),
                        optionSourceRegistry));
            }
            projected.add(Collections.unmodifiableMap(output));
        }
        return List.copyOf(projected);
    }

    private static Object titleValue(OptionFieldDefinition definition,
                                     Object value,
                                     OptionSourceRegistry optionSourceRegistry) {
        Map<String, String> titles = optionSourceRegistry.source(definition.binding()).options(OptionQuery.all()).stream()
                .collect(Collectors.toMap(OptionItem::code, OptionItem::title, (left, right) -> left));
        return definition.selectionMode() == OptionSelectionMode.MULTIPLE
                ? multipleTitles(value, titles)
                : singleTitle(value, titles);
    }

    private static String singleTitle(Object value, Map<String, String> titles) {
        String code = code(value);
        return code == null ? null : titles.get(code);
    }

    private static List<String> multipleTitles(Object value, Map<String, String> titles) {
        if (value == null) {
            return null;
        }
        List<String> resolved = new ArrayList<>();
        for (Object item : toValues(value)) {
            String code = code(item);
            String title = code == null ? null : titles.get(code);
            if (title != null) {
                resolved.add(title);
            }
        }
        return resolved;
    }

    private static String code(Object value) {
        if (value instanceof CodeTitleEnum codeTitleEnum) {
            return codeTitleEnum.getCode();
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isBlank() ? null : trimmed;
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        return null;
    }

    private static List<?> toValues(Object value) {
        if (value instanceof Iterable<?> iterable) {
            List<Object> values = new ArrayList<>();
            iterable.forEach(values::add);
            return values;
        }
        if (value.getClass().isArray()) {
            List<Object> values = new ArrayList<>();
            for (int i = 0; i < Array.getLength(value); i++) {
                values.add(Array.get(value, i));
            }
            return values;
        }
        return List.of();
    }
}
