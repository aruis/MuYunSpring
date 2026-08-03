package net.ximatai.muyun.spring.platform.dictionary;

import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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

    /** Creates a seed whose internal category ID is safely derived from its stable dictionary identity. */
    public static DictionarySeed dictionaryFor(String applicationAlias,
                                              String alias,
                                              String title,
                                              int sortOrder,
                                              DictionaryItemSeed... items) {
        String source = requireText(applicationAlias, "applicationAlias") + "."
                + requireText(alias, "dictionaryCategoryAlias");
        return dictionary(internalId("dict.", source, 24), applicationAlias, alias, title, sortOrder, items);
    }

    static String itemId(String applicationAlias, String alias, String code) {
        String source = requireText(applicationAlias, "applicationAlias") + "."
                + requireText(alias, "dictionaryCategoryAlias") + "."
                + requireText(code, "dictionaryItemCode");
        return internalId("dict.item.", source, 22);
    }

    private static String internalId(String prefix, String source, int digestLength) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return prefix + HexFormat.of().formatHex(bytes).substring(0, digestLength);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
