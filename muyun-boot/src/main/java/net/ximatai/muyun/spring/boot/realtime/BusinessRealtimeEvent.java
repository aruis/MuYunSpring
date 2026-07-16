package net.ximatai.muyun.spring.boot.realtime;

public record BusinessRealtimeEvent(
        String type,
        String moduleAlias,
        String recordId,
        String reason,
        String sensitivity
) {
    private static final String DIRTY_MARKER_SENSITIVITY = "DIRTY_MARKER";

    public BusinessRealtimeEvent {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("business realtime event type must not be blank");
        }
        if (moduleAlias == null || moduleAlias.isBlank()) {
            throw new IllegalArgumentException("business realtime event moduleAlias must not be blank");
        }
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("business realtime event recordId must not be blank");
        }
        type = type.trim();
        moduleAlias = moduleAlias.trim();
        recordId = recordId.trim();
        reason = reason == null || reason.isBlank() ? null : reason.trim();
        sensitivity = sensitivity == null || sensitivity.isBlank() ? DIRTY_MARKER_SENSITIVITY : sensitivity.trim();
    }

    public static BusinessRealtimeEvent userSessionCollectionChanged(String userId, String reason) {
        return new BusinessRealtimeEvent(
                "iam.user.session.collectionChanged", "iam.user", userId, reason, DIRTY_MARKER_SENSITIVITY);
    }
}
