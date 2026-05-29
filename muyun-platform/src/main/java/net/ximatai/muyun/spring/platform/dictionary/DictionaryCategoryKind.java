package net.ximatai.muyun.spring.platform.dictionary;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum DictionaryCategoryKind implements CodeTitleEnum {
    FOLDER("folder", "目录"),
    DICTIONARY("dictionary", "字典");

    private final String code;
    private final String title;

    DictionaryCategoryKind(String code, String title) {
        this.code = code;
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }
}
