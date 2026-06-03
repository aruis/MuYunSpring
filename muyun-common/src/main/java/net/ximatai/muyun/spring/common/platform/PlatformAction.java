package net.ximatai.muyun.spring.common.platform;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public enum PlatformAction {
    CREATE(PlatformActionGroup.CRUD, "create", PlatformActionKind.RECORD, "Create",
            PlatformActionLevel.LIST, PlatformActionStyle.PRIMARY, 10),
    VIEW(PlatformActionGroup.CRUD, "view", PlatformActionKind.RECORD, "View",
            PlatformActionLevel.RECORD, PlatformActionStyle.NORMAL, 20),
    UPDATE(PlatformActionGroup.CRUD, "update", PlatformActionKind.RECORD, "Update",
            PlatformActionLevel.DEFAULT, PlatformActionStyle.NORMAL, 30),
    DELETE(PlatformActionGroup.CRUD, "delete", PlatformActionKind.RECORD, "Delete",
            PlatformActionLevel.DEFAULT, PlatformActionStyle.DANGER, 40),
    QUERY(PlatformActionGroup.CRUD, "query", PlatformActionKind.COLLECTION, "Query",
            PlatformActionLevel.DEFAULT, PlatformActionStyle.NORMAL, 50),

    SORT(PlatformActionGroup.SORT, "sort", PlatformActionKind.SORT, "Sort",
            PlatformActionLevel.DEFAULT, PlatformActionStyle.NORMAL, 10),

    TREE(PlatformActionGroup.TREE, "tree", PlatformActionKind.TREE, "Tree",
            PlatformActionLevel.DEFAULT, PlatformActionStyle.NORMAL, 10),

    REFERENCE(PlatformActionGroup.REFERENCE, "reference", PlatformActionKind.REFERENCE, "Reference",
            PlatformActionLevel.DEFAULT, PlatformActionStyle.NORMAL, 10),

    ENABLE(PlatformActionGroup.ENABLE, "enable", PlatformActionKind.STATE, "Enable",
            PlatformActionLevel.DEFAULT, PlatformActionStyle.NORMAL, 10),
    DISABLE(PlatformActionGroup.ENABLE, "disable", PlatformActionKind.STATE, "Disable",
            PlatformActionLevel.DEFAULT, PlatformActionStyle.NORMAL, 20);

    private final PlatformActionGroup group;
    private final String code;
    private final PlatformActionKind kind;
    private final String title;
    private final PlatformActionLevel level;
    private final PlatformActionStyle style;
    private final int order;

    PlatformAction(PlatformActionGroup group,
                   String code,
                   PlatformActionKind kind,
                   String title,
                   PlatformActionLevel level,
                   PlatformActionStyle style,
                   int order) {
        this.group = group;
        this.code = code;
        this.kind = kind;
        this.title = title;
        this.level = level;
        this.style = style;
        this.order = order;
    }

    public PlatformActionGroup group() {
        return group;
    }

    public String code() {
        return code;
    }

    public PlatformActionKind kind() {
        return kind;
    }

    public String title() {
        return title;
    }

    public PlatformActionLevel level() {
        return level;
    }

    public PlatformActionStyle style() {
        return style;
    }

    public int order() {
        return order;
    }

    public boolean matches(String actionCode) {
        return code.equals(actionCode);
    }

    public static Optional<PlatformAction> fromCode(String actionCode) {
        return Arrays.stream(values())
                .filter(action -> action.matches(actionCode))
                .findFirst();
    }

    public static List<PlatformAction> ofGroup(PlatformActionGroup group) {
        return Arrays.stream(values())
                .filter(action -> action.group == group)
                .sorted(Comparator.comparingInt(PlatformAction::order))
                .toList();
    }
}
