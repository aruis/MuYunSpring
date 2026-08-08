package net.ximatai.muyun.spring.platform.option;

import net.ximatai.muyun.spring.ability.option.StaticOptionLoadPopulator;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionLoadDefinition;
import net.ximatai.muyun.spring.common.option.OptionLoadResolver;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import net.ximatai.muyun.spring.common.option.OptionValueCodeResolver;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OptionSourceStaticOptionLoadPopulator implements StaticOptionLoadPopulator {
    private final OptionSourceRegistry optionSourceRegistry;

    public OptionSourceStaticOptionLoadPopulator(OptionSourceRegistry optionSourceRegistry) {
        this.optionSourceRegistry = optionSourceRegistry;
    }

    @Override
    public void populate(Class<?> modelClass, Object entity) {
        if (entity == null) {
            return;
        }
        populateAll(modelClass, List.of(entity));
    }

    @Override
    public void populateAll(Class<?> modelClass, List<?> entities) {
        if (modelClass == null || entities == null || entities.isEmpty()) {
            return;
        }
        for (OptionLoadDefinition definition : OptionLoadResolver.resolve(modelClass)) {
            Map<String, OptionItem> options = optionItems(definition);
            for (Object entity : entities) {
                populate(modelClass, entity, definition, options);
            }
        }
    }

    private Map<String, OptionItem> optionItems(OptionLoadDefinition definition) {
        return optionSourceRegistry.source(definition.sourceDefinition().binding()).options(OptionQuery.all()).stream()
                .collect(Collectors.toMap(OptionItem::code, item -> item, (left, right) -> left));
    }

    private void populate(Class<?> modelClass,
                          Object entity,
                          OptionLoadDefinition definition,
                          Map<String, OptionItem> options) {
        if (entity == null) {
            return;
        }
        Object sourceValue = readField(modelClass, entity, definition.sourceField());
        Object loaded = definition.sourceDefinition().selectionMode() == OptionSelectionMode.MULTIPLE
                ? multipleValues(definition, sourceValue, definition.optionItemField(), options)
                : singleValue(definition, sourceValue, definition.optionItemField(), options);
        writeField(modelClass, entity, definition.outputField(), loaded);
    }

    private Object singleValue(OptionLoadDefinition definition,
                               Object value,
                               String itemField,
                               Map<String, OptionItem> options) {
        String code = OptionValueCodeResolver.resolve(definition.sourceDefinition().binding(), value);
        OptionItem item = code == null ? null : options.get(code);
        return item == null ? null : optionItemValue(item, itemField);
    }

    private List<Object> multipleValues(OptionLoadDefinition definition,
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

    private Object optionItemValue(OptionItem item, String fieldName) {
        for (RecordComponent component : OptionItem.class.getRecordComponents()) {
            if (component.getName().equals(fieldName)) {
                try {
                    return component.getAccessor().invoke(item);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException("cannot read option item field: " + fieldName, ex);
                }
            }
        }
        throw new IllegalArgumentException("unknown option item field: " + fieldName);
    }

    private List<?> toValues(Object value) {
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

    private Object readField(Class<?> modelClass, Object entity, String fieldName) {
        try {
            Field field = findField(modelClass, fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("cannot read option field: " + fieldName, ex);
        }
    }

    private void writeField(Class<?> modelClass, Object entity, String fieldName, Object value) {
        try {
            Field field = findField(modelClass, fieldName);
            field.setAccessible(true);
            field.set(entity, value);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("cannot write option load field: " + fieldName, ex);
        }
    }

    private Field findField(Class<?> modelClass, String fieldName) {
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new IllegalArgumentException("unknown option field: " + fieldName);
    }
}
