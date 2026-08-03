package net.ximatai.muyun.spring.common.option;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

/** Compiles {@link DictionaryField} declarations without introducing a dictionary runtime dependency. */
public final class DictionaryFieldResolver {
    private DictionaryFieldResolver() {
    }

    public static List<DictionaryFieldDefinition> resolve(Class<?> modelClass) {
        List<DictionaryFieldDefinition> definitions = new java.util.ArrayList<>();
        for (Field field : OptionFieldResolver.fields(modelClass)) {
            resolve(field).ifPresent(definitions::add);
        }
        return List.copyOf(definitions);
    }

    public static Optional<DictionaryFieldDefinition> resolve(Field field) {
        if (field == null) {
            return Optional.empty();
        }
        DictionaryField annotation = field.getAnnotation(DictionaryField.class);
        if (annotation == null) {
            return Optional.empty();
        }
        OptionBinding binding = new OptionBinding(OptionBinding.DICTIONARY_SOURCE, annotation.source());
        binding.dictionarySource();
        OptionFieldDefinition optionDefinition = OptionFieldResolver.definition(
                field, binding, annotation.selectionMode());
        List<DictionaryInitialItemDefinition> items = java.util.Arrays.stream(annotation.initialItems())
                .map(item -> new DictionaryInitialItemDefinition(item.code(), item.title(), item.sortOrder()))
                .toList();
        return Optional.of(new DictionaryFieldDefinition(field.getName(), optionDefinition,
                annotation.title(), annotation.sortOrder(), items));
    }

}
