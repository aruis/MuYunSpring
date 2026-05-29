package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

public record FieldDictionaryBinding(String applicationAlias, String categoryAlias) {
    public FieldDictionaryBinding {
        applicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        categoryAlias = PlatformNameRules.requireIdentifier(categoryAlias, "dictionaryCategoryAlias");
    }
}
