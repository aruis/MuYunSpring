package net.ximatai.muyun.spring.common.exception;

public record ErrorScope(String moduleAlias,
                         String entityAlias,
                         String actionCode) {
    public static ErrorScope empty() {
        return new ErrorScope(null, null, null);
    }

    public static ErrorScope module(String moduleAlias) {
        return new ErrorScope(moduleAlias, null, null);
    }

    public ErrorScope entity(String entityAlias) {
        return new ErrorScope(moduleAlias, entityAlias, actionCode);
    }

    public ErrorScope action(String actionCode) {
        return new ErrorScope(moduleAlias, entityAlias, actionCode);
    }

    public boolean isEmpty() {
        return isBlank(moduleAlias) && isBlank(entityAlias) && isBlank(actionCode);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
