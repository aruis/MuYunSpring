package net.ximatai.muyun.spring.boot;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import net.ximatai.muyun.spring.ability.TransactionScopeSupport;

@ApplicationScoped
public class QuarkusTransactionScopeConfiguration {
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @Inject
    public QuarkusTransactionScopeConfiguration(TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
    }

    @PostConstruct
    void configure() {
        TransactionScopeSupport.configureTransactionAdapter(new TransactionScopeSupport.TransactionAdapter() {
            @Override
            public boolean isTransactionActive() {
                return transactionSynchronizationRegistry.getTransactionStatus() == Status.STATUS_ACTIVE;
            }

            @Override
            public boolean registerAfterCommit(Runnable action) {
                if (!isTransactionActive()) {
                    return false;
                }
                transactionSynchronizationRegistry.registerInterposedSynchronization(new Synchronization() {
                    @Override
                    public void beforeCompletion() {
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status == Status.STATUS_COMMITTED) {
                            try {
                                action.run();
                            } catch (RuntimeException e) {
                                throw new TransactionScopeSupport.AfterCommitActionException(e);
                            }
                        }
                    }
                });
                return true;
            }
        });
    }

    @PreDestroy
    void reset() {
        TransactionScopeSupport.resetTransactionAdapter();
    }
}
