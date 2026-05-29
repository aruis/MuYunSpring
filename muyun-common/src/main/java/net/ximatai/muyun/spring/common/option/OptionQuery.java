package net.ximatai.muyun.spring.common.option;

public record OptionQuery(boolean onlyEnabled, String parentCode) {
    public static OptionQuery all() {
        return new OptionQuery(false, null);
    }

    public static OptionQuery enabledOnly() {
        return new OptionQuery(true, null);
    }

    public OptionQuery childrenOf(String code) {
        return new OptionQuery(onlyEnabled, code);
    }
}
