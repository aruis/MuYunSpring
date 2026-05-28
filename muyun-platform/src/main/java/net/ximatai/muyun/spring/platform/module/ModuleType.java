package net.ximatai.muyun.spring.platform.module;

public enum ModuleType {
    STANDARD("standard", "标准页面"),
    CUSTOMIZED("customized", "自定义页面"),
    BUILT_IN_PAGE("built_in_page", "内置界面"),
    REPORT("report", "报表页面"),
    DASHBOARD("dashboard", "仪表板");

    private final String code;
    private final String title;

    ModuleType(String code, String title) {
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
