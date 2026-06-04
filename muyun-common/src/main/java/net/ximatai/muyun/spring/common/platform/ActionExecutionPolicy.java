package net.ximatai.muyun.spring.common.platform;

public record ActionExecutionPolicy(
        String actionCode,
        PlatformActionLevel level,
        ActionAccessMode accessMode,
        boolean actionAuth,
        boolean dataAuth,
        ActionDefaultGrantPolicy defaultGrantPolicy,
        String inheritActionCode
) {
    public ActionExecutionPolicy {
        actionCode = requireText(actionCode, "actionCode");
        level = level == null ? PlatformActionLevel.DEFAULT : level;
        accessMode = accessMode == null ? ActionAccessMode.AUTH_REQUIRED : accessMode;
        defaultGrantPolicy = defaultGrantPolicy == null ? ActionDefaultGrantPolicy.NONE : defaultGrantPolicy;
        inheritActionCode = normalizeBlank(inheritActionCode);
        if (accessMode == ActionAccessMode.ANONYMOUS_ALLOWED && (actionAuth || dataAuth || inheritActionCode != null)) {
            throw new IllegalArgumentException("anonymous action cannot require auth or data scope: " + actionCode);
        }
        if (accessMode == ActionAccessMode.LOGIN_REQUIRED && (actionAuth || dataAuth || inheritActionCode != null)) {
            throw new IllegalArgumentException("login-only action cannot require action auth or data scope: " + actionCode);
        }
        if (defaultGrantPolicy.requiresDataScope() && !dataAuth) {
            throw new IllegalArgumentException("scoped default grant requires dataAuth: " + actionCode);
        }
        if (!actionAuth && inheritActionCode != null) {
            throw new IllegalArgumentException("action inherit requires actionAuth: " + actionCode);
        }
    }

    public static ActionExecutionPolicy standard(PlatformAction action) {
        java.util.Objects.requireNonNull(action, "action must not be null");
        return new ActionExecutionPolicy(
                action.code(),
                action.level(),
                action.accessMode(),
                action.actionAuth(),
                action.dataAuth(),
                action.defaultGrantPolicy(),
                action.inheritActionCode()
        );
    }

    public String permissionActionCode() {
        return inheritActionCode == null ? actionCode : inheritActionCode;
    }

    public boolean requiresLogin() {
        return accessMode != ActionAccessMode.ANONYMOUS_ALLOWED;
    }

    public boolean requiresDataScope() {
        return dataAuth;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private static String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
