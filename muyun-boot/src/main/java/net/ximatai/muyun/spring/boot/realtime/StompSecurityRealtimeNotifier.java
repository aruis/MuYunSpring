package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.common.web.RequestTraceContext;

public class StompSecurityRealtimeNotifier implements SecurityRealtimeNotifier {
    public static final String MESSAGE_TYPE = "platform.security-notification";

    private final RealtimeMessagePublisher messagePublisher;

    public StompSecurityRealtimeNotifier(RealtimeMessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @Override
    public void notifyPasswordChanged(String userId) {
        send(userId, SecurityNotification.passwordChanged());
    }

    @Override
    public void notifyPasswordReset(String userId) {
        send(userId, SecurityNotification.passwordReset());
    }

    @Override
    public void notifyForceLogout(String userId) {
        send(userId, SecurityNotification.forceLogout());
    }

    private void send(String userId, SecurityNotification notification) {
        if (userId == null || userId.isBlank()) {
            return;
        }
        String traceId = RequestTraceContext.currentTraceId().orElse(null);
        messagePublisher.sendToUser(userId, RealtimeDestinations.USER_NOTIFICATIONS,
                RealtimeEnvelope.of(MESSAGE_TYPE, traceId, notification));
    }
}
