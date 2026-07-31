package net.ximatai.muyun.spring.web.realtime;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;

public interface DataChangeRealtimePublisher {
    void publish(CommittedChangeSet changeSet);
}
