package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;

public class StompDataChangeRealtimePublisher implements DataChangeRealtimePublisher {
    public static final String MESSAGE_TYPE = "platform.data-change";

    private final RealtimeMessagePublisher messagePublisher;

    public StompDataChangeRealtimePublisher(RealtimeMessagePublisher messagePublisher) {
        this.messagePublisher = messagePublisher;
    }

    @Override
    public void publish(CommittedChangeSet changeSet) {
        if (changeSet == null || changeSet.changes().isEmpty()) {
            return;
        }
        CurrentUser currentUser = CurrentUserContext.currentUser().orElse(null);
        if (currentUser == null) {
            return;
        }
        String traceId = RequestTraceContext.currentTraceId().orElse(null);
        messagePublisher.sendToUser(currentUser.userId(), RealtimeDestinations.DATA_CHANGES,
                RealtimeEnvelope.of(MESSAGE_TYPE, traceId, changeSet));
    }
}
