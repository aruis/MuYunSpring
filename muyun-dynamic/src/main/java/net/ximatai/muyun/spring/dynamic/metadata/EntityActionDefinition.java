package net.ximatai.muyun.spring.dynamic.metadata;

public record EntityActionDefinition(
        String entityCode,
        String actionCode,
        EntityActionKind kind,
        String title,
        boolean enabled,
        EntityActionLevel level,
        String permissionCode,
        String availableExpression,
        String unavailableMessage
) {
    public EntityActionDefinition {
        level = level == null ? EntityActionLevel.NORMAL : level;
    }

    public EntityActionDefinition(String entityCode,
                                  String actionCode,
                                  EntityActionKind kind,
                                  String title,
                                  boolean enabled,
                                  EntityActionLevel level,
                                  String permissionCode) {
        this(entityCode, actionCode, kind, title, enabled, level, permissionCode, null, null);
    }

    public static EntityActionDefinition enabled(String entityCode,
                                                 String actionCode,
                                                 EntityActionKind kind,
                                                 String title) {
        return new EntityActionDefinition(entityCode, actionCode, kind, title, true, EntityActionLevel.NORMAL, null);
    }

    public EntityActionDefinition availableWhen(String expression) {
        return availableWhen(expression, null);
    }

    public EntityActionDefinition availableWhen(String expression, String message) {
        return new EntityActionDefinition(entityCode, actionCode, kind, title, enabled, level, permissionCode,
                expression, message);
    }

    public boolean hasAvailabilityCondition() {
        return availableExpression != null && !availableExpression.isBlank();
    }
}
