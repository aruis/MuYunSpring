package net.ximatai.muyun.spring.dynamic.metadata;

import java.util.Set;

public final class DynamicActionPathRules {
    private static final Set<String> RESERVED_WEB_ACTION_CODES = Set.of(
            "actions", "delete", "describe", "disable", "enable", "entities", "insert", "openapi", "query", "references", "tree", "update", "view"
    );

    private DynamicActionPathRules() {
    }

    public static boolean isReservedWebActionCode(String actionCode) {
        return RESERVED_WEB_ACTION_CODES.contains(actionCode);
    }

    public static Set<String> reservedWebActionCodes() {
        return RESERVED_WEB_ACTION_CODES;
    }
}
