package net.ximatai.muyun.spring.boot.realtime;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;

public interface DataChangeRealtimePublisher {
    void publish(CommittedChangeSet changeSet);
}
