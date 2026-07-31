package net.ximatai.muyun.spring.web.realtime;

public record RealtimeQueue(String destination) {
    public RealtimeQueue {
        destination = requireDestination(destination, "/queue/");
    }

    public static RealtimeQueue of(String destination) {
        return new RealtimeQueue(destination);
    }

    private static String requireDestination(String value, String prefix) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("realtime destination must not be blank");
        }
        String normalized = value.trim();
        if (!normalized.startsWith(prefix)) {
            throw new IllegalArgumentException("realtime queue must start with " + prefix);
        }
        return normalized;
    }
}
