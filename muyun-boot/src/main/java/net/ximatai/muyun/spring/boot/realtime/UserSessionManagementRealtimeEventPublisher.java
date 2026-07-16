package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.iam.user.UserSessionLifecycleEvent;
import org.springframework.context.event.EventListener;

import java.util.Objects;

public class UserSessionManagementRealtimeEventPublisher {
    private static final String USER_MODULE_ALIAS = "iam.user";
    private static final String SESSIONS_ACTION = "sessions";

    private final BusinessRealtimeFanOutPublisher fanOutPublisher;
    private final BusinessRealtimeRecipientPolicyFactory recipientPolicyFactory;

    public UserSessionManagementRealtimeEventPublisher(
            BusinessRealtimeFanOutPublisher fanOutPublisher,
            BusinessRealtimeRecipientPolicyFactory recipientPolicyFactory) {
        this.fanOutPublisher = Objects.requireNonNull(fanOutPublisher, "fanOutPublisher must not be null");
        this.recipientPolicyFactory = Objects.requireNonNull(
                recipientPolicyFactory, "recipientPolicyFactory must not be null");
    }

    @EventListener
    public void publish(UserSessionLifecycleEvent event) {
        if (event == null) {
            return;
        }
        BusinessRealtimeEvent notification = BusinessRealtimeEvent.userSessionCollectionChanged(
                event.userId(), event.type().name());
        fanOutPublisher.publish(notification, recipientPolicyFactory.recordAction(
                USER_MODULE_ALIAS, event.userId(), SESSIONS_ACTION));
    }
}
