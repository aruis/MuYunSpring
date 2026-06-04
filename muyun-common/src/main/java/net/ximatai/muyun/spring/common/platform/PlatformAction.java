package net.ximatai.muyun.spring.common.platform;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public enum PlatformAction {
    CREATE(PlatformActionGroup.CRUD, "create", "Create",
            PlatformActionLevel.LIST, 10,
            ActionAccessMode.AUTH_REQUIRED, true, false, ActionDefaultPolicy.NONE, null),
    VIEW(PlatformActionGroup.CRUD, "view", "View",
            PlatformActionLevel.RECORD, 20,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, null),
    UPDATE(PlatformActionGroup.CRUD, "update", "Update",
            PlatformActionLevel.RECORD, 30,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, null),
    DELETE(PlatformActionGroup.CRUD, "delete", "Delete",
            PlatformActionLevel.RECORD, 40,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, null),
    QUERY(PlatformActionGroup.CRUD, "query", "Query",
            PlatformActionLevel.LIST, 50,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, VIEW),

    SORT(PlatformActionGroup.SORT, "sort", "Sort",
            PlatformActionLevel.RECORD, 10,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, null),

    TREE(PlatformActionGroup.TREE, "tree", "Tree",
            PlatformActionLevel.LIST, 10,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, VIEW),

    REFERENCE(PlatformActionGroup.REFERENCE, "reference", "Reference",
            PlatformActionLevel.LIST, 10,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, VIEW),

    ENABLE(PlatformActionGroup.ENABLE, "enable", "Enable",
            PlatformActionLevel.RECORD, 10,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, null),
    DISABLE(PlatformActionGroup.ENABLE, "disable", "Disable",
            PlatformActionLevel.RECORD, 20,
            ActionAccessMode.AUTH_REQUIRED, true, true, ActionDefaultPolicy.NONE, ENABLE);

    private final PlatformActionGroup group;
    private final String code;
    private final String title;
    private final PlatformActionLevel level;
    private final int order;
    private final ActionAccessMode accessMode;
    private final boolean actionAuth;
    private final boolean dataAuth;
    private final ActionDefaultPolicy defaultPolicy;
    private final PlatformAction permissionAction;

    PlatformAction(PlatformActionGroup group,
                   String code,
                   String title,
                   PlatformActionLevel level,
                   int order) {
        this(group, code, title, level, order,
                ActionAccessMode.AUTH_REQUIRED, true, false, ActionDefaultPolicy.NONE, null);
    }

    PlatformAction(PlatformActionGroup group,
                   String code,
                   String title,
                   PlatformActionLevel level,
                   int order,
                   ActionAccessMode accessMode,
                   boolean actionAuth,
                   boolean dataAuth,
                   ActionDefaultPolicy defaultPolicy,
                   PlatformAction permissionAction) {
        this.group = group;
        this.code = code;
        this.title = title;
        this.level = level;
        this.order = order;
        this.accessMode = accessMode;
        this.actionAuth = actionAuth;
        this.dataAuth = dataAuth;
        this.defaultPolicy = defaultPolicy;
        this.permissionAction = permissionAction;
    }

    public PlatformActionGroup group() {
        return group;
    }

    public String code() {
        return code;
    }

    public String title() {
        return title;
    }

    public PlatformActionLevel level() {
        return level;
    }

    public int order() {
        return order;
    }

    public ActionAccessMode accessMode() {
        return accessMode;
    }

    public boolean actionAuth() {
        return actionAuth;
    }

    public boolean dataAuth() {
        return dataAuth;
    }

    public ActionDefaultPolicy defaultPolicy() {
        return defaultPolicy;
    }

    public String permissionActionCode() {
        return permissionAction == null ? code : permissionAction.code();
    }

    public String inheritActionCode() {
        return permissionAction == null ? null : permissionAction.code();
    }

    public ActionExecutionPolicy executionPolicy() {
        return ActionExecutionPolicy.standard(this);
    }

    public boolean matches(String actionCode) {
        return code.equals(actionCode);
    }

    public static Optional<PlatformAction> fromCode(String actionCode) {
        return Arrays.stream(values())
                .filter(action -> action.matches(actionCode))
                .findFirst();
    }

    public static String permissionActionCodeOf(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            throw new IllegalArgumentException("actionCode must not be blank");
        }
        String validActionCode = actionCode.trim();
        return fromCode(validActionCode).map(PlatformAction::permissionActionCode).orElse(validActionCode);
    }

    public static List<PlatformAction> ofGroup(PlatformActionGroup group) {
        return Arrays.stream(values())
                .filter(action -> action.group == group)
                .sorted(Comparator.comparingInt(PlatformAction::order))
                .toList();
    }
}
