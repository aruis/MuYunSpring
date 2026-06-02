package net.ximatai.muyun.spring.ability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionScopeSupportTest {
    @BeforeEach
    void setUp() {
        clearTransactionState();
    }

    @AfterEach
    void tearDown() {
        clearTransactionState();
    }

    private void clearTransactionState() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldRunImmediatelyWhenNoTransactionIsActive() {
        AtomicInteger calls = new AtomicInteger();

        TransactionScopeSupport.afterCommitOrNow(calls::incrementAndGet);

        assertThat(calls).hasValue(1);
    }

    @Test
    void shouldRunAfterCommitWhenSpringTransactionSynchronizationIsActive() {
        AtomicInteger calls = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TransactionScopeSupport.afterCommitOrNow(calls::incrementAndGet);

        assertThat(calls).hasValue(0);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(calls).hasValue(1);
    }

    @Test
    void shouldNotRunWhenSpringTransactionRollsBack() {
        AtomicInteger calls = new AtomicInteger();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TransactionScopeSupport.afterCommitOrNow(calls::incrementAndGet);

        assertThat(calls).hasValue(0);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertThat(calls).hasValue(0);
    }

    @Test
    void shouldMarkAfterCommitActionFailure() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        TransactionScopeSupport.afterCommitOrNow(() -> {
            throw new IllegalStateException("event failed");
        });

        assertThatThrownBy(() -> TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit))
                .isInstanceOf(TransactionScopeSupport.AfterCommitActionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
