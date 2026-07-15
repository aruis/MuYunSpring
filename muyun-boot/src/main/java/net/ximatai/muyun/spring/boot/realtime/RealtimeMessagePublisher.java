package net.ximatai.muyun.spring.boot.realtime;

public interface RealtimeMessagePublisher {
    void broadcast(RealtimeTopic topic, Object payload);

    void sendToUser(String userId, RealtimeQueue queue, Object payload);
}
