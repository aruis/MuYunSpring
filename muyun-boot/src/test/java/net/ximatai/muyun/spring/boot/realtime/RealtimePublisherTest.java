package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.DataChange;
import net.ximatai.muyun.spring.common.identity.CurrentUser;
import net.ximatai.muyun.spring.common.identity.CurrentUserContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RealtimePublisherTest {
    @Test
    void shouldSendDataChangeEnvelopeToCurrentUserQueue() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompDataChangeRealtimePublisher publisher = new StompDataChangeRealtimePublisher(messagePublisher);
        CommittedChangeSet changeSet = new CommittedChangeSet("change-set-1",
                List.of(DataChange.recordUpdated("iam.employee", "employee-1")));

        try (CurrentUserContext.Scope ignored = CurrentUserContext.use(
                CurrentUser.tenantUser("user-1", "User", "tenant-a"))) {
            publisher.publish(changeSet);
        }

        assertThat(messagePublisher.userId).isEqualTo("user-1");
        assertThat(messagePublisher.queue).isEqualTo(RealtimeDestinations.DATA_CHANGES);
        assertThat(messagePublisher.payload).isInstanceOf(RealtimeEnvelope.class);
        RealtimeEnvelope<?> envelope = (RealtimeEnvelope<?>) messagePublisher.payload;
        assertThat(envelope.type()).isEqualTo(StompDataChangeRealtimePublisher.MESSAGE_TYPE);
        assertThat(envelope.payload()).isEqualTo(changeSet);
    }

    @Test
    void shouldSkipDataChangeWithoutCurrentUser() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompDataChangeRealtimePublisher publisher = new StompDataChangeRealtimePublisher(messagePublisher);

        publisher.publish(new CommittedChangeSet("change-set-1",
                List.of(DataChange.recordUpdated("iam.employee", "employee-1"))));

        assertThat(messagePublisher.payload).isNull();
    }

    @Test
    void shouldSkipEmptyChangeSet() {
        RecordingRealtimeMessagePublisher messagePublisher = new RecordingRealtimeMessagePublisher();
        StompDataChangeRealtimePublisher publisher = new StompDataChangeRealtimePublisher(messagePublisher);

        publisher.publish(CommittedChangeSet.empty("change-set-1"));

        assertThat(messagePublisher.payload).isNull();
    }

    private static final class RecordingRealtimeMessagePublisher implements RealtimeMessagePublisher {
        private RealtimeTopic topic;
        private String userId;
        private RealtimeQueue queue;
        private Object payload;
        private final List<String> users = new ArrayList<>();

        @Override
        public void broadcast(RealtimeTopic topic, Object payload) {
            this.topic = topic;
            this.payload = payload;
        }

        @Override
        public void sendToUser(String userId, RealtimeQueue queue, Object payload) {
            this.userId = userId;
            this.queue = queue;
            this.payload = payload;
            users.add(userId);
        }
    }
}
