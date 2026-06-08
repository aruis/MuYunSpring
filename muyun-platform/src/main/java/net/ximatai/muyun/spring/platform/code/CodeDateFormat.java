package net.ximatai.muyun.spring.platform.code;

public enum CodeDateFormat {
    YY("yy"),
    YYYY("yyyy"),
    YYMM("yyMM"),
    YYYYMM("yyyyMM"),
    YYMMDD("yyMMdd"),
    YYYYMMDD("yyyyMMdd"),
    YYYY_MM("yyyy-MM"),
    YYYY_MM_DD("yyyy-MM-dd"),
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),
    HHMMSS("HHmmss");

    private final String pattern;

    CodeDateFormat(String pattern) {
        this.pattern = pattern;
    }

    public String pattern() {
        return pattern;
    }
}
