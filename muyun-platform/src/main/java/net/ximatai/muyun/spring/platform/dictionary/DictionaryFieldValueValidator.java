package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.spring.dynamic.metadata.FieldDictionaryBinding;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.runtime.DynamicFieldValueValidator;
import org.springframework.stereotype.Component;

@Component
public class DictionaryFieldValueValidator implements DynamicFieldValueValidator {
    private final DictionaryItemService itemService;

    public DictionaryFieldValueValidator(DictionaryItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public void validate(String moduleAlias,
                         EntityDefinition entity,
                         FieldDefinition field,
                         Object value) {
        FieldDictionaryBinding binding = field.dictionaryBinding();
        if (binding == null || value == null) {
            return;
        }
        String code = String.valueOf(value);
        if (itemService.resolveEnabledItem(binding.applicationAlias(), binding.categoryAlias(), code) == null) {
            throw new IllegalArgumentException("invalid dictionary code: " + code);
        }
    }
}
