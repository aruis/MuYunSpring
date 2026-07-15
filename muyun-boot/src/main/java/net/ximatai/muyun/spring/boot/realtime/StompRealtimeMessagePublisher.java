package net.ximatai.muyun.spring.boot.realtime;

import org.springframework.messaging.simp.SimpMessagingTemplate;

public class StompRealtimeMessagePublisher implements RealtimeMessagePublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public StompRealtimeMessagePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void broadcast(RealtimeTopic topic, Object payload) {
        messagingTemplate.convertAndSend(topic.destination(), payload);
    }

    @Override
    public void sendToUser(String userId, RealtimeQueue queue, Object payload) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        messagingTemplate.convertAndSendToUser(userId.trim(), queue.destination(), payload);
    }
}
