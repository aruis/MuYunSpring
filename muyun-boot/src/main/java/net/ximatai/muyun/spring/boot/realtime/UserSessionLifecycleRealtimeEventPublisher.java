package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;
import net.ximatai.muyun.spring.iam.user.UserAccountService;
import net.ximatai.muyun.spring.iam.user.UserSessionLifecycleEvent;
import net.ximatai.muyun.spring.iam.user.UserSessionLifecycleEventPublisher;

import java.util.List;
import java.util.UUID;

public class UserSessionLifecycleRealtimeEventPublisher implements UserSessionLifecycleEventPublisher {
    private final RealtimeMessagePublisher messagePublisher;

    public UserSessionLifecycleRealtimeEventPublisher(RealtimeMessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @Override
    public void publish(UserSessionLifecycleEvent event) {
        if (event == null) {
            return;
        }
        CommittedChangeSet changeSet = new CommittedChangeSet(
                "iam-user-session-" + UUID.randomUUID(),
                List.of(DataChange.sessionCollectionChanged(
                        UserAccountService.MODULE_ALIAS,
                        event.userId(),
                        event.sessionId()))
        );
        String traceId = RequestTraceContext.currentTraceId().orElse(null);
        messagePublisher.broadcast(
                RealtimeDestinations.moduleDataChanges(UserAccountService.MODULE_ALIAS),
                RealtimeEnvelope.of(StompDataChangeRealtimePublisher.MESSAGE_TYPE, traceId, changeSet)
        );
    }
}
