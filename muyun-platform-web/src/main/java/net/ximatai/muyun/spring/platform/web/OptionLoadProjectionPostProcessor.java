package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.option.OptionLoadDefinition;
import net.ximatai.muyun.spring.common.option.OptionLoadResolver;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.option.OptionValueCodeResolver;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class OptionLoadProjectionPostProcessor {
    private OptionLoadProjectionPostProcessor() {
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
        java.util.Set<String> optionLoadFields = graph.parsedTransforms().stream()
                .filter(RecordReadPostTransform::isOptionLoad)
                .map(RecordReadPostTransform::fieldName)
                .collect(Collectors.toUnmodifiableSet());
        if (optionLoadFields.isEmpty()) {
            return records;
        }
        List<OptionLoadDefinition> definitions = OptionLoadResolver.resolve(modelClass).stream()
                .filter(definition -> optionLoadFields.contains(definition.outputField()))
                .toList();
        if (definitions.isEmpty()) {
            return records;
        }
        List<Map<String, Object>> projected = new ArrayList<>(records.size());
        for (Map<String, Object> record : records) {
            LinkedHashMap<String, Object> output = new LinkedHashMap<>(record);
            for (OptionLoadDefinition definition : definitions) {
                if (!output.containsKey(definition.sourceField()) || output.get(definition.outputField()) != null) {
                    continue;
                }
                output.put(definition.outputField(), loadedValue(definition, output.get(definition.sourceField()),
                        optionSourceRegistry));
            }
            projected.add(Collections.unmodifiableMap(output));
        }
        return List.copyOf(projected);
    }

    private static Object loadedValue(OptionLoadDefinition definition,
                                     Object value,
                                     OptionSourceRegistry optionSourceRegistry) {
        Map<String, OptionItem> options = optionSourceRegistry.source(definition.sourceDefinition().binding())
                .options(OptionQuery.all()).stream()
                .collect(Collectors.toMap(OptionItem::code, item -> item, (left, right) -> left));
        return definition.sourceDefinition().selectionMode() == OptionSelectionMode.MULTIPLE
                ? multipleValues(definition, value, definition.optionItemField(), options)
                : singleValue(definition, value, definition.optionItemField(), options);
    }

    private static Object singleValue(OptionLoadDefinition definition,
                                      Object value,
                                      String itemField,
                                      Map<String, OptionItem> options) {
        String code = OptionValueCodeResolver.resolve(definition.sourceDefinition().binding(), value);
        OptionItem item = code == null ? null : options.get(code);
        return item == null ? null : optionItemValue(item, itemField);
    }

    private static List<Object> multipleValues(OptionLoadDefinition definition,
                                               Object value,
                                               String itemField,
                                               Map<String, OptionItem> options) {
        if (value == null) {
            return null;
        }
        List<Object> resolved = new ArrayList<>();
        for (Object item : toValues(value)) {
            Object loaded = singleValue(definition, item, itemField, options);
            if (loaded != null) {
                resolved.add(loaded);
            }
        }
        return resolved;
    }

    private static Object optionItemValue(OptionItem item, String fieldName) {
        return switch (fieldName) {
            case "code" -> item.code();
            case "title" -> item.title();
            case "enabled" -> item.enabled();
            case "sortOrder" -> item.sortOrder();
            case "parentCode" -> item.parentCode();
            default -> throw new IllegalArgumentException("unknown option item field: " + fieldName);
        };
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
