package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum MeasureUnitConversionScopeType implements CodeTitleEnum {
    GLOBAL("global", "Global"),
    MODULE("module", "Module"),
    RECORD_CONTEXT("recordContext", "Record context");

    private final String code;
    private final String title;

    MeasureUnitConversionScopeType(String code, String title) {
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
