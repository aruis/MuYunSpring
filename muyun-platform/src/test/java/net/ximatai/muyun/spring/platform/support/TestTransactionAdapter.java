package net.ximatai.muyun.spring.platform.support;

import net.ximatai.muyun.spring.ability.TransactionScopeSupport;

import java.util.ArrayList;
import java.util.List;

public final class TestTransactionAdapter implements TransactionScopeSupport.TransactionAdapter {
    private final List<Runnable> afterCommitActions = new ArrayList<>();
    private boolean active;

    public void begin() {
        active = true;
    }

    public void commit() {
        List<Runnable> actions = List.copyOf(afterCommitActions);
        afterCommitActions.clear();
        active = false;
        actions.forEach(Runnable::run);
    }

    public void rollback() {
        afterCommitActions.clear();
        active = false;
    }

    public int pendingActions() {
        return afterCommitActions.size();
    }

    @Override
    public boolean isTransactionActive() {
        return active;
    }

    @Override
    public boolean registerAfterCommit(Runnable action) {
        if (!active) {
            return false;
        }
        afterCommitActions.add(() -> {
            try {
                action.run();
            } catch (RuntimeException e) {
                throw new TransactionScopeSupport.AfterCommitActionException(e);
            }
        });
        return true;
    }
}
