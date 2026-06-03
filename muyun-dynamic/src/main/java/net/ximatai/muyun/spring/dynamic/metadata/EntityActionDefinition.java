package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.common.platform.PlatformAction;

public record EntityActionDefinition(
        String entityAlias,
        String actionCode,
        EntityActionKind kind,
        String title,
        boolean enabled,
        EntityActionLevel level,
        EntityActionStyle style,
        EntityActionCategory category,
        EntityActionAccessMode accessMode,
        Boolean actionAuth,
        Boolean dataAuth,
        String authInheritActionCode,
        String availableExpression,
        String unavailableMessage,
        EntityActionExecutorType executorType,
        String executorKey
) {
    public EntityActionDefinition {
        level = level == null ? defaultLevel(actionCode, kind) : level;
        style = style == null ? EntityActionStyle.NORMAL : style;
        category = category == null ? defaultCategory(kind) : category;
        accessMode = accessMode == null ? EntityActionAccessMode.AUTH_REQUIRED : accessMode;
        actionAuth = actionAuth == null ? accessMode == EntityActionAccessMode.AUTH_REQUIRED : actionAuth;
        dataAuth = dataAuth != null && dataAuth;
        executorType = executorType == null ? defaultExecutorType(category) : executorType;
    }

    public EntityActionDefinition(String entityAlias,
                                  String actionCode,
                                  EntityActionKind kind,
                                  String title,
                                  boolean enabled,
                                  EntityActionStyle style) {
        this(entityAlias, actionCode, kind, title, enabled, null, style, null, null,
                null, null, null, null, null, null, null);
    }

    public static EntityActionDefinition enabled(String entityAlias,
                                                 String actionCode,
                                                 EntityActionKind kind,
                                                 String title) {
        return new EntityActionDefinition(entityAlias, actionCode, kind, title, true, EntityActionStyle.NORMAL);
    }

    public EntityActionDefinition availableWhen(String expression) {
        return availableWhen(expression, null);
    }

    public EntityActionDefinition availableWhen(String expression, String message) {
        return new EntityActionDefinition(entityAlias, actionCode, kind, title, enabled, level, style, category,
                accessMode, actionAuth, dataAuth, authInheritActionCode, expression, message, executorType, executorKey);
    }

    public boolean hasAvailabilityCondition() {
        return availableExpression != null && !availableExpression.isBlank();
    }

    public static EntityActionLevel defaultLevel(String actionCode, EntityActionKind kind) {
        if (PlatformAction.CREATE.matches(actionCode)) {
            return EntityActionLevel.LIST;
        }
        if (kind == EntityActionKind.RECORD || kind == EntityActionKind.STATE) {
            return EntityActionLevel.RECORD;
        }
        if (kind == EntityActionKind.CUSTOM) {
            return EntityActionLevel.ANY;
        }
        return EntityActionLevel.LIST;
    }

    public static EntityActionCategory defaultCategory(EntityActionKind kind) {
        return kind == EntityActionKind.CUSTOM ? EntityActionCategory.CUSTOM : EntityActionCategory.STANDARD;
    }

    public static EntityActionExecutorType defaultExecutorType(EntityActionCategory category) {
        return switch (category) {
            case DIALOG -> EntityActionExecutorType.DIALOG;
            case WORKFLOW -> EntityActionExecutorType.WORKFLOW;
            case GENERATE -> EntityActionExecutorType.GENERATE;
            case CUSTOM -> EntityActionExecutorType.SERVICE;
            case STANDARD -> EntityActionExecutorType.STANDARD;
        };
    }
}
