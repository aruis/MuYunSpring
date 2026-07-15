package net.ximatai.muyun.spring.boot.realtime;

public record RealtimeCommand(String destination) {
    public RealtimeCommand {
        destination = requireDestination(destination, "/app/");
    }

    public static RealtimeCommand of(String destination) {
        return new RealtimeCommand(destination);
    }

    private static String requireDestination(String value, String prefix) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("realtime destination must not be blank");
        }
        String normalized = value.trim();
        if (!normalized.startsWith(prefix)) {
            throw new IllegalArgumentException("realtime command must start with " + prefix);
        }
        return normalized;
    }
}
