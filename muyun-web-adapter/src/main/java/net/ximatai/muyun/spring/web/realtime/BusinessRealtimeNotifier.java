package net.ximatai.muyun.spring.web.realtime;

public interface BusinessRealtimeNotifier {
    void notifyUser(String userId, BusinessRealtimeEvent event);
}
