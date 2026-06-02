package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.option.OptionSelectionMode;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record FieldDictionaryBinding(String applicationAlias, String categoryAlias, OptionSelectionMode selectionMode) {
    public FieldDictionaryBinding {
        applicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        categoryAlias = PlatformNameRules.requireIdentifier(categoryAlias, "dictionaryCategoryAlias");
        selectionMode = selectionMode == null ? OptionSelectionMode.SINGLE : selectionMode;
    }

    public FieldDictionaryBinding(String applicationAlias, String categoryAlias) {
        this(applicationAlias, categoryAlias, OptionSelectionMode.SINGLE);
    }

    public OptionBinding toOptionBinding() {
        return OptionBinding.dictionary(applicationAlias, categoryAlias);
    }
}
