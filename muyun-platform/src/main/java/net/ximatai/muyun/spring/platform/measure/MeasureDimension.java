package net.ximatai.muyun.spring.platform.measure;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum MeasureDimension implements CodeTitleEnum {
    LENGTH("length", "Length"),
    MASS("mass", "Mass"),
    AREA("area", "Area"),
    VOLUME("volume", "Volume"),
    COUNT("count", "Count"),
    TIME("time", "Time"),
    CUSTOM("custom", "Custom");

    private final String code;
    private final String title;

    MeasureDimension(String code, String title) {
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
