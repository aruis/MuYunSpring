package net.ximatai.muyun.spring.dynamic.metadata;

public record EntityActionDefinition(
        String entityCode,
        String actionCode,
        EntityActionKind kind,
        String title,
        boolean enabled,
        EntityActionLevel level,
        String permissionCode
) {
    public EntityActionDefinition {
        level = level == null ? EntityActionLevel.NORMAL : level;
    }

    public static EntityActionDefinition enabled(String entityCode,
                                                 String actionCode,
                                                 EntityActionKind kind,
                                                 String title) {
        return new EntityActionDefinition(entityCode, actionCode, kind, title, true, EntityActionLevel.NORMAL, null);
    }
}
