package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

final class StaticProjectionSqlNames {
    static final String MAIN_ALIAS = "main";

    private StaticProjectionSqlNames() {
    }

    static String requireAlias(String value, String name) {
        if (MAIN_ALIAS.equals(value)) {
            return value;
        }
        return PlatformNameRules.requireIdentifier(value, name);
    }

    static String requireColumn(String value, String name) {
        return PlatformNameRules.requireDatabaseName(value, name);
    }
}
