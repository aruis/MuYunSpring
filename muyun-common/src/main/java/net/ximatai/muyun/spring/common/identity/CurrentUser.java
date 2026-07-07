package net.ximatai.muyun.spring.common.identity;

public record CurrentUser(
        String userId,
        String username,
        String tenantId,
        String organizationId,
        boolean system,
        boolean passwordChangeRequired
) {
    public CurrentUser {
        userId = requireText(userId, "userId");
        username = normalize(username);
        tenantId = normalize(tenantId);
        organizationId = normalize(organizationId);
    }

    public static CurrentUser tenantUser(String userId, String username, String tenantId) {
        return tenantUser(userId, username, tenantId, null);
    }

    public static CurrentUser tenantUser(String userId, String username, String tenantId, String organizationId) {
        return tenantUser(userId, username, tenantId, organizationId, false);
    }

    public static CurrentUser tenantUser(String userId,
                                         String username,
                                         String tenantId,
                                         String organizationId,
                                         boolean passwordChangeRequired) {
        return new CurrentUser(userId, username, tenantId, organizationId, false, passwordChangeRequired);
    }

    public static CurrentUser systemUser(String userId, String username) {
        return systemUser(userId, username, false);
    }

    public static CurrentUser systemUser(String userId, String username, boolean passwordChangeRequired) {
        return new CurrentUser(userId, username, null, null, true, passwordChangeRequired);
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
