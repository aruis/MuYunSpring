package net.ximatai.muyun.spring.common.platform;

public record RecordActionAvailabilityDecision(
        boolean available,
        String reason
) {
    public RecordActionAvailabilityDecision {
        reason = normalize(reason);
    }

    public static RecordActionAvailabilityDecision allow() {
        return new RecordActionAvailabilityDecision(true, null);
    }

    public static RecordActionAvailabilityDecision unavailable(String reason) {
        return new RecordActionAvailabilityDecision(false, reason);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
