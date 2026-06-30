package net.ximatai.muyun.spring.platform.dictionary;

public record DictionaryItemSeed(
        String id,
        String code,
        String title,
        int sortOrder
) {
    public DictionaryItemSeed {
        code = requireText(code, "dictionaryItemCode");
        title = requireText(title, "dictionaryItemTitle");
    }

    public static DictionaryItemSeed item(String code, String title, int sortOrder) {
        return new DictionaryItemSeed(null, code, title, sortOrder);
    }

    public DictionaryItemSeed withId(String value) {
        return new DictionaryItemSeed(requireText(value, "dictionaryItemId"), code, title, sortOrder);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
