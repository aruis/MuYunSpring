package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import net.ximatai.muyun.spring.common.web.RequestTraceContext;

import java.util.LinkedHashSet;
import java.util.Set;

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
        RealtimeEnvelope<CommittedChangeSet> envelope = RealtimeEnvelope.of(MESSAGE_TYPE, traceId, changeSet);
        messagePublisher.sendToUser(currentUser.userId(), RealtimeDestinations.DATA_CHANGES, envelope);
        broadcastChangeSet(changeSet, envelope);
    }

    private void broadcastChangeSet(CommittedChangeSet changeSet, RealtimeEnvelope<CommittedChangeSet> envelope) {
        Set<String> moduleAliases = new LinkedHashSet<>();
        Set<RecordTopicKey> recordTopics = new LinkedHashSet<>();
        for (DataChange change : changeSet.changes()) {
            moduleAliases.add(change.moduleAlias());
            if (change.recordId() != null) {
                recordTopics.add(new RecordTopicKey(change.moduleAlias(), change.recordId()));
            }
        }
        moduleAliases.forEach(moduleAlias ->
                messagePublisher.broadcast(RealtimeDestinations.moduleDataChanges(moduleAlias), envelope));
        recordTopics.forEach(topic ->
                messagePublisher.broadcast(RealtimeDestinations.recordDataChanges(topic.moduleAlias(), topic.recordId()),
                        envelope));
    }

    private record RecordTopicKey(String moduleAlias, String recordId) {
    }
}
