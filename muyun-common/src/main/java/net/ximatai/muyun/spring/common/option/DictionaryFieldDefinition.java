package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.util.Preconditions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Compiled static dictionary-field declaration. */
public record DictionaryFieldDefinition(
        String fieldName,
        OptionFieldDefinition optionDefinition,
        String title,
        int sortOrder,
        List<DictionaryInitialItemDefinition> initialItems
) {
    public DictionaryFieldDefinition {
        fieldName = Preconditions.requireText(fieldName, "dictionaryFieldName");
        if (optionDefinition == null || !OptionBinding.DICTIONARY_SOURCE.equals(optionDefinition.binding().sourceType())) {
            throw new IllegalArgumentException("dictionary field must use dictionary option binding");
        }
        title = title == null ? "" : title.trim();
        initialItems = initialItems == null ? List.of() : List.copyOf(initialItems);
        if (!initialItems.isEmpty() && title.isBlank()) {
            throw new IllegalArgumentException("dictionary field with initialItems requires title: " + fieldName);
        }
        Set<String> codes = new HashSet<>();
        for (DictionaryInitialItemDefinition item : initialItems) {
            if (item == null || !codes.add(item.code())) {
                throw new IllegalArgumentException("dictionary initial item code must be unique: " + fieldName);
            }
        }
    }

    public OptionBinding binding() {
        return optionDefinition.binding();
    }

    public boolean declaresBaseline() {
        return !title.isBlank();
    }
}
