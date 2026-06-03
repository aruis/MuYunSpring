package net.ximatai.muyun.spring.common.web;

import java.util.Set;

public final class PlatformWebPathRules {
    public static final String ACTION_CODE_PATH =
            "{actionCode:^(?!actions$|delete$|describe$|disable$|enable$|entities$|insert$|openapi$|query$|references$|sort$|tree$|update$|view$)[A-Za-z][A-Za-z0-9_]*$}";

    private static final Set<String> RESERVED_WEB_ACTION_CODES = Set.of(
            "actions", "delete", "describe", "disable", "enable", "entities", "insert", "openapi", "query", "references", "sort", "tree", "update", "view"
    );

    private PlatformWebPathRules() {
    }

    public static boolean isReservedWebActionCode(String actionCode) {
        return RESERVED_WEB_ACTION_CODES.contains(actionCode);
    }

    public static Set<String> reservedWebActionCodes() {
        return RESERVED_WEB_ACTION_CODES;
    }
}
