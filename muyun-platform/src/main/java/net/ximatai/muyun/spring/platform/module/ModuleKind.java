package net.ximatai.muyun.spring.platform.module;

public enum ModuleKind {
    STATIC("static", "静态模块"),
    DYNAMIC("dynamic", "动态模块");

    private final String code;
    private final String title;

    ModuleKind(String code, String title) {
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
