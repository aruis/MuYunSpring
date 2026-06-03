package net.ximatai.muyun.spring.common.identity;

public record CurrentUser(
        String userId,
        String username,
        String tenantId,
        String organizationId,
        boolean system
) {
    public CurrentUser {
        userId = requireText(userId, "userId");
        username = normalize(username);
        tenantId = normalize(tenantId);
        organizationId = normalize(organizationId);
    }

    public static CurrentUser tenantUser(String userId, String username, String tenantId) {
        return new CurrentUser(userId, username, tenantId, null, false);
    }

    public static CurrentUser tenantUser(String userId, String username, String tenantId, String organizationId) {
        return new CurrentUser(userId, username, tenantId, organizationId, false);
    }

    public static CurrentUser systemUser(String userId, String username) {
        return new CurrentUser(userId, username, null, null, true);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
