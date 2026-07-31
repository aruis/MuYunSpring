package net.ximatai.muyun.spring.web.realtime;

public interface RealtimeMessagePublisher {
    void broadcast(RealtimeTopic topic, Object payload);

    void sendToUser(String userId, RealtimeQueue queue, Object payload);
}
