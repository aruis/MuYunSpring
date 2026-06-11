package net.ximatai.muyun.spring.common.web;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class PlatformWebPathRules {
    public static final String ACTION_CODE_PATH =
            "{actionCode:^(?!actions$|delete$|describe$|disable$|enable$|entities$|exchange$|export$|import$|insert$|openapi$|query$|reference$|references$|sort$|tree$|update$|view$)[A-Za-z][A-Za-z0-9_]*$}";

    private static final Set<String> RESERVED_WEB_ACTION_CODES = reservedCodes();

    private PlatformWebPathRules() {
    }

    public static boolean isReservedWebActionCode(String actionCode) {
        return RESERVED_WEB_ACTION_CODES.contains(actionCode);
    }

    public static boolean isStandardActionPathCode(String actionCode) {
        return PlatformAction.BATCH_DELETE.matches(actionCode);
    }

    public static Set<String> reservedWebActionCodes() {
        return RESERVED_WEB_ACTION_CODES;
    }

    private static Set<String> reservedCodes() {
        Set<String> codes = Arrays.stream(PlatformAction.values())
                .map(PlatformAction::code)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        codes.addAll(Set.of("actions", "describe", "entities", "exchange", "openapi", "references"));
        return Set.copyOf(codes);
    }
}
