package net.ximatai.muyun.spring.ability.event;

import net.ximatai.muyun.spring.ability.TransactionScopeSupport;

public interface RuntimeEventPublisher {
    RuntimeEventPublisher NOOP = event -> {
    };

    void publish(RuntimeEvent event);

    default void publishAfterCommit(RuntimeEvent event) {
        TransactionScopeSupport.afterCommitOrNow(() -> publish(event));
    }

    static RuntimeEventPublisher noop() {
        return NOOP;
    }
}
