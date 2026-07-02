package net.ximatai.muyun.spring.ability;

import java.util.Objects;

public final class TransactionScopeSupport {
    private static final TransactionAdapter NO_TRANSACTION_ADAPTER = new TransactionAdapter() {
        @Override
        public boolean isTransactionActive() {
            return false;
        }

        @Override
        public boolean registerAfterCommit(Runnable action) {
            return false;
        }
    };

    private static volatile TransactionAdapter transactionAdapter = NO_TRANSACTION_ADAPTER;

    private TransactionScopeSupport() {
    }

    public static boolean isTransactionActive() {
        return transactionAdapter.isTransactionActive();
    }

    public static void afterCommitOrNow(Runnable action) {
        if (!transactionAdapter.registerAfterCommit(action)) {
            action.run();
        }
    }

    public static void configureTransactionAdapter(TransactionAdapter adapter) {
        transactionAdapter = Objects.requireNonNull(adapter, "adapter");
    }

    public static void resetTransactionAdapter() {
        transactionAdapter = NO_TRANSACTION_ADAPTER;
    }

    public interface TransactionAdapter {
        boolean isTransactionActive();

        boolean registerAfterCommit(Runnable action);
    }

    public static final class AfterCommitActionException extends RuntimeException {
        public AfterCommitActionException(RuntimeException cause) {
            super(cause);
        }

        public RuntimeException unwrap() {
            return (RuntimeException) getCause();
        }
    }
}
