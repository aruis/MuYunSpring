package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import net.ximatai.muyun.spring.common.util.Preconditions;

import java.util.Objects;

public record OptionBinding(String sourceType, String source) {
    public static final String DICTIONARY_SOURCE = "dictionary";
    public static final String ENUM_SOURCE = "enum";

    public OptionBinding {
        sourceType = Preconditions.requireText(sourceType, "sourceType");
        source = Preconditions.requireText(source, "source");
    }

    public static OptionBinding dictionary(String applicationAlias, String categoryAlias) {
        String validApplicationAlias = PlatformNameRules.requireApplicationAlias(applicationAlias);
        String validCategoryAlias = PlatformNameRules.requireIdentifier(categoryAlias, "dictionaryCategoryAlias");
        return new OptionBinding(DICTIONARY_SOURCE, validApplicationAlias + "." + validCategoryAlias);
    }

    public static <E extends Enum<E> & CodeTitleEnum> OptionBinding enumType(Class<E> enumType) {
        return new OptionBinding(ENUM_SOURCE, Objects.requireNonNull(enumType, "enumType must not be null").getName());
    }
}
