package net.ximatai.muyun.spring.ability;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionScopeSupportTest {
    private final TestTransactionAdapter transactionAdapter = new TestTransactionAdapter();

    @BeforeEach
    void setUp() {
        TransactionScopeSupport.configureTransactionAdapter(transactionAdapter);
    }

    @AfterEach
    void tearDown() {
        TransactionScopeSupport.resetTransactionAdapter();
    }

    @Test
    void shouldRunImmediatelyWhenNoTransactionIsActive() {
        AtomicInteger calls = new AtomicInteger();

        TransactionScopeSupport.afterCommitOrNow(calls::incrementAndGet);

        assertThat(calls).hasValue(1);
    }

    @Test
    void shouldRunAfterCommitWhenTransactionSynchronizationIsActive() {
        AtomicInteger calls = new AtomicInteger();
        transactionAdapter.begin();

        TransactionScopeSupport.afterCommitOrNow(calls::incrementAndGet);

        assertThat(calls).hasValue(0);
        transactionAdapter.commit();
        assertThat(calls).hasValue(1);
    }

    @Test
    void shouldNotRunWhenTransactionRollsBack() {
        AtomicInteger calls = new AtomicInteger();
        transactionAdapter.begin();

        TransactionScopeSupport.afterCommitOrNow(calls::incrementAndGet);

        assertThat(calls).hasValue(0);
        transactionAdapter.rollback();
        assertThat(calls).hasValue(0);
    }

    @Test
    void shouldMarkAfterCommitActionFailure() {
        transactionAdapter.begin();

        TransactionScopeSupport.afterCommitOrNow(() -> {
            throw new IllegalStateException("event failed");
        });

        assertThatThrownBy(transactionAdapter::commit)
                .isInstanceOf(TransactionScopeSupport.AfterCommitActionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
