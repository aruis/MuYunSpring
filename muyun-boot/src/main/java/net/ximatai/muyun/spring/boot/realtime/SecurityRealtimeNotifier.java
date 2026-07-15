package net.ximatai.muyun.spring.boot.realtime;

public interface SecurityRealtimeNotifier {
    void notifyPasswordChanged(String userId);

    void notifyPasswordReset(String userId);

    void notifyForceLogout(String userId);

    void notifySessionRevoked(String userId, String sessionId);
}
