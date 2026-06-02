package net.ximatai.muyun.spring.common.option;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum OptionSelectionMode implements CodeTitleEnum {
    SINGLE("single", "单选"),
    MULTIPLE("multiple", "多选");

    private final String code;
    private final String title;

    OptionSelectionMode(String code, String title) {
        this.code = code;
        this.title = title;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
