package net.ximatai.muyun.spring.platform.dictionary;

import java.util.Arrays;
import java.util.List;

public record DictionarySeed(
        String id,
        String applicationAlias,
        String alias,
        String title,
        int sortOrder,
        List<DictionaryItemSeed> items
) {
    public DictionarySeed {
        id = requireText(id, "dictionaryCategoryId");
        applicationAlias = requireText(applicationAlias, "applicationAlias");
        alias = requireText(alias, "dictionaryCategoryAlias");
        title = requireText(title, "dictionaryCategoryTitle");
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static DictionarySeed dictionary(String id,
                                            String applicationAlias,
                                            String alias,
                                            String title,
                                            int sortOrder,
                                            DictionaryItemSeed... items) {
        return new DictionarySeed(id, applicationAlias, alias, title, sortOrder,
                items == null ? List.of() : Arrays.asList(items));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
