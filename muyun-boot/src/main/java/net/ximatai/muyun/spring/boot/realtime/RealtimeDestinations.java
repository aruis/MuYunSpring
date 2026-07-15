package net.ximatai.muyun.spring.boot.realtime;

public final class RealtimeDestinations {
    public static final RealtimeQueue DATA_CHANGES = RealtimeQueue.of("/queue/platform/data-changes");
    public static final RealtimeQueue USER_NOTIFICATIONS = RealtimeQueue.of("/queue/platform/notifications");
    public static final RealtimeCommand PLATFORM_PING = RealtimeCommand.of("/app/platform/ping");

    private RealtimeDestinations() {
    }
}
