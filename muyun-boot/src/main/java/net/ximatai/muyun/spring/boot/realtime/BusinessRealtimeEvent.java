package net.ximatai.muyun.spring.boot.realtime;

public record BusinessRealtimeEvent(
        String type,
        String moduleAlias,
        String recordId,
        String reason
) {
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
    }

    public static BusinessRealtimeEvent userSessionChanged(String userId, String reason) {
        return new BusinessRealtimeEvent("iam.user.session.changed", "iam.user", userId, reason);
    }
}
