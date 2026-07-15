package net.ximatai.muyun.spring.boot.realtime;

public interface BusinessRealtimeNotifier {
    void notifyUser(String userId, BusinessRealtimeEvent event);
}
