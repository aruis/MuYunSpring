package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.util.PlatformNameRules;

final class RelationProjectionSqlNames {
    static final String MAIN_ALIAS = "main";

    private RelationProjectionSqlNames() {
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
