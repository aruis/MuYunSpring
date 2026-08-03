package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldOptionLoadDefinition;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Resolves dynamic option-load virtual fields through the shared option-source registry. */
public final class OptionSourceDynamicOptionLoadPopulator implements DynamicOptionLoadPopulator {
    private final OptionSourceRegistry optionSourceRegistry;

    public OptionSourceDynamicOptionLoadPopulator(OptionSourceRegistry optionSourceRegistry) {
        this.optionSourceRegistry = optionSourceRegistry;
    }

    @Override
    public void populate(EntityDefinition entity, List<DynamicRecord> records) {
        if (entity == null || records == null || records.isEmpty()) {
            return;
        }
        for (FieldDefinition output : entity.fields()) {
            FieldOptionLoadDefinition load = output.optionLoad();
            if (load == null) {
                continue;
            }
            FieldDefinition source = entity.fields().stream()
                    .filter(field -> field.fieldName().equals(load.sourceField()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("unknown option load source: " + load.sourceField()));
            Map<String, OptionItem> options = optionSourceRegistry.source(source.optionBinding()).options(OptionQuery.all())
                    .stream().collect(Collectors.toMap(OptionItem::code, item -> item, (left, right) -> left));
            for (DynamicRecord record : records) {
                Object value = source.dictionaryBinding().selectionMode() == OptionSelectionMode.MULTIPLE
                        ? multipleValue(record.getValue(source.fieldName()), load.optionItemField(), options)
                        : singleValue(record.getValue(source.fieldName()), load.optionItemField(), options);
                record.putVirtualValue(output.fieldName(), value);
            }
        }
    }

    private Object singleValue(Object value, String itemField, Map<String, OptionItem> options) {
        if (!(value instanceof String code) || code.isBlank()) {
            return null;
        }
        OptionItem item = options.get(code.trim());
        return item == null ? null : optionItemValue(item, itemField);
    }

    private List<Object> multipleValue(Object value, String itemField, Map<String, OptionItem> options) {
        if (!(value instanceof Iterable<?> values)) {
            return value == null ? null : List.of();
        }
        List<Object> loaded = new ArrayList<>();
        for (Object candidate : values) {
            Object item = singleValue(candidate, itemField, options);
            if (item != null) {
                loaded.add(item);
            }
        }
        return loaded;
    }

    private Object optionItemValue(OptionItem item, String fieldName) {
        for (RecordComponent component : OptionItem.class.getRecordComponents()) {
            if (component.getName().equals(fieldName)) {
                try {
                    return component.getAccessor().invoke(item);
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("cannot read option item field: " + fieldName, exception);
                }
            }
        }
        throw new IllegalArgumentException("unknown option item field: " + fieldName);
    }
}
