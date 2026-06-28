package net.ximatai.muyun.spring.platform.option;

import net.ximatai.muyun.spring.ability.option.StaticOptionFieldValueValidator;
import net.ximatai.muyun.spring.common.option.OptionFieldDefinition;
import net.ximatai.muyun.spring.common.option.OptionFieldResolver;
import net.ximatai.muyun.spring.common.option.OptionItem;
import net.ximatai.muyun.spring.common.option.OptionQuery;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.option.OptionSource;
import net.ximatai.muyun.spring.common.option.OptionSourceRegistry;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class OptionSourceStaticFieldValueValidator implements StaticOptionFieldValueValidator {
    private final OptionSourceRegistry optionSourceRegistry;

    public OptionSourceStaticFieldValueValidator(OptionSourceRegistry optionSourceRegistry) {
        this.optionSourceRegistry = optionSourceRegistry;
    }

    @Override
    public void validate(Class<?> modelClass, Object entity) {
        if (modelClass == null || entity == null) {
            return;
        }
        for (OptionFieldDefinition definition : OptionFieldResolver.resolve(modelClass)) {
            Object value = readField(modelClass, entity, definition.fieldName());
            if (value == null) {
                continue;
            }
            OptionSource source = optionSourceRegistry.source(definition.binding());
            if (definition.selectionMode() == OptionSelectionMode.MULTIPLE) {
                validateMultiple(definition, source, value);
                continue;
            }
            String code = normalizeSingleValue(definition, value);
            if (code != null) {
                validateCode(definition, source, code);
            }
        }
    }

    private void validateMultiple(OptionFieldDefinition definition, OptionSource source, Object value) {
        List<?> values = toValues(value);
        Set<String> seen = new LinkedHashSet<>();
        for (Object item : values) {
            String code = normalizeSingleValue(definition, item);
            if (code == null) {
                throw new IllegalArgumentException("multiple option field requires non-blank code: "
                        + definition.fieldName());
            }
            if (!seen.add(code)) {
                throw new IllegalArgumentException("duplicate option code for field "
                        + definition.fieldName() + ": " + code);
            }
            validateCode(definition, source, code);
        }
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
        throw new IllegalArgumentException("multiple option field requires collection");
    }

    private String normalizeSingleValue(OptionFieldDefinition definition, Object value) {
        if (!(value instanceof String code)) {
            throw new IllegalArgumentException("option field requires string code: " + definition.fieldName());
        }
        String trimmed = code.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateCode(OptionFieldDefinition definition, OptionSource source, String code) {
        boolean exists = source.options(OptionQuery.enabledOnly()).stream()
                .map(OptionItem::code)
                .anyMatch(code::equals);
        if (!exists) {
            throw new IllegalArgumentException("invalid option code for field "
                    + definition.fieldName() + ": " + code);
        }
    }

    private Object readField(Class<?> modelClass, Object entity, String fieldName) {
        Field field = findField(modelClass, fieldName);
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("cannot read option field: " + fieldName, ex);
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
