package net.ximatai.muyun.spring.web.realtime;

import net.ximatai.muyun.spring.common.web.RequestTraceContext;

public class StompBusinessRealtimeNotifier implements BusinessRealtimeNotifier {
    public static final String MESSAGE_TYPE = "platform.business-event";

    private final RealtimeMessagePublisher messagePublisher;

    public StompBusinessRealtimeNotifier(RealtimeMessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @Override
    public void notifyUser(String userId, BusinessRealtimeEvent event) {
        if (userId == null || userId.isBlank() || event == null) {
            return;
        }
        messagePublisher.sendToUser(userId, RealtimeDestinations.USER_BUSINESS_EVENTS,
                RealtimeEnvelope.of(MESSAGE_TYPE, RequestTraceContext.currentTraceId().orElse(null), event));
    }
}
