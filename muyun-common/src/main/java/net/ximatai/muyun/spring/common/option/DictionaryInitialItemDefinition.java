package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.util.Preconditions;

/** A startup baseline item declared by {@link DictionaryField}. */
public record DictionaryInitialItemDefinition(String code, String title, int sortOrder) {
    public DictionaryInitialItemDefinition {
        code = Preconditions.requireText(code, "dictionaryItemCode");
        title = Preconditions.requireText(title, "dictionaryItemTitle");
    }
}
