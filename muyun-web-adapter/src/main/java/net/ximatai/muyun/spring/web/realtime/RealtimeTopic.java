package net.ximatai.muyun.spring.web.realtime;

public record RealtimeTopic(String destination) {
    public RealtimeTopic {
        destination = requireDestination(destination, "/topic/");
    }

    public static RealtimeTopic of(String destination) {
        return new RealtimeTopic(destination);
    }

    private static String requireDestination(String value, String prefix) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("realtime destination must not be blank");
        }
        String normalized = value.trim();
        if (!normalized.startsWith(prefix)) {
            throw new IllegalArgumentException("realtime topic must start with " + prefix);
        }
        return normalized;
    }
}
