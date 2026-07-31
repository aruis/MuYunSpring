package net.ximatai.muyun.spring.web.realtime;

import net.ximatai.muyun.spring.common.identity.CurrentUser;

public interface BusinessRealtimeFanOutPublisher {
    void publish(BusinessRealtimeEvent event, RecipientPolicy recipientPolicy);

    @FunctionalInterface
    interface RecipientPolicy {
        boolean canReceive(CurrentUser currentUser);
    }
}
