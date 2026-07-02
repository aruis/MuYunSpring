package net.ximatai.muyun.spring.platform.option;

import net.ximatai.muyun.spring.ability.option.StaticOptionFieldTitlePopulator;
import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.common.option.OptionFieldDefinition;
import net.ximatai.muyun.spring.common.option.OptionFieldResolver;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import jakarta.enterprise.context.Dependent;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Dependent
public class OptionSourceStaticFieldTitlePopulator implements StaticOptionFieldTitlePopulator {
    private final OptionSourceRegistry optionSourceRegistry;

    public OptionSourceStaticFieldTitlePopulator(OptionSourceRegistry optionSourceRegistry) {
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
        for (OptionFieldDefinition definition : titleOutputDefinitions(modelClass)) {
            Map<String, String> titles = optionTitles(definition);
            for (Object entity : entities) {
                populateTitle(modelClass, entity, definition, titles);
            }
        }
    }

    private List<OptionFieldDefinition> titleOutputDefinitions(Class<?> modelClass) {
        return OptionFieldResolver.resolve(modelClass).stream()
                .filter(OptionFieldDefinition::hasTitleOutput)
                .toList();
    }

    private Map<String, String> optionTitles(OptionFieldDefinition definition) {
        return optionSourceRegistry.source(definition.binding()).options(OptionQuery.all()).stream()
                .collect(Collectors.toMap(OptionItem::code, OptionItem::title, (left, right) -> left));
    }

    private void populateTitle(Class<?> modelClass,
                               Object entity,
                               OptionFieldDefinition definition,
                               Map<String, String> titles) {
        if (entity == null) {
            return;
        }
        Object value = readField(modelClass, entity, definition.fieldName());
        Object titleValue = definition.selectionMode() == OptionSelectionMode.MULTIPLE
                ? multipleTitles(value, titles)
                : singleTitle(value, titles);
        writeField(modelClass, entity, definition.titleOutputField(), titleValue);
    }

    private String singleTitle(Object value, Map<String, String> titles) {
        String code = code(value);
        return code == null ? null : titles.get(code);
    }

    private List<String> multipleTitles(Object value, Map<String, String> titles) {
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

    private String code(Object value) {
        if (value instanceof CodeTitleEnum codeTitleEnum) {
            return codeTitleEnum.getCode();
        }
        if (value instanceof String text) {
            String trimmed = text.trim();
            return trimmed.isBlank() ? null : trimmed;
        }
        return null;
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
            throw new IllegalStateException("cannot write option title field: " + fieldName, ex);
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
