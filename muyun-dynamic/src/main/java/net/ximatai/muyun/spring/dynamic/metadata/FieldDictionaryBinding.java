package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record FieldDictionaryBinding(String applicationAlias, String categoryAlias) {
    public FieldDictionaryBinding {
        applicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        categoryAlias = PlatformNameRules.requireIdentifier(categoryAlias, "dictionaryCategoryAlias");
    }

    public OptionBinding toOptionBinding() {
        return OptionBinding.dictionary(applicationAlias, categoryAlias);
    }
}
