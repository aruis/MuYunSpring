package net.ximatai.muyun.spring.ability.action;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MutationContextTest {
    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldCommitAllRecordedChangesWithoutTransaction() {
        MutationContext context = new MutationContext("change-set-1");

        context.record(new DataChangeIntent(DataChangeOperation.CREATED, FirstModule.class, "record-1"));
        context.record(new DataChangeIntent(DataChangeOperation.UPDATED, SecondModule.class, "record-2"));

        CommittedChangeSet changeSet = context.committedChangeSet(moduleType -> {
            if (moduleType == FirstModule.class) {
                return "demo.first";
            }
            if (moduleType == SecondModule.class) {
                return "demo.second";
            }
            throw new IllegalArgumentException(moduleType.getName());
        });

        assertThat(changeSet.changeSetId()).isEqualTo("change-set-1");
        assertThat(changeSet.changes())
                .extracting(DataChange::type, DataChange::moduleAlias, DataChange::recordId)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("record-created", "demo.first", "record-1"),
                        org.assertj.core.groups.Tuple.tuple("record-updated", "demo.second", "record-2")
                );
    }

    @Test
    void shouldRunCommitCallbackAfterTransactionCommit() {
        MutationContext context = new MutationContext("change-set-1");
        List<CommittedChangeSet> published = new ArrayList<>();

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        context.record(DataChange.recordUpdated("iam.employee", "employee-1"));
        context.afterCommit(null, published::add);

        assertThat(published).isEmpty();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        TransactionSynchronizationManager.setActualTransactionActive(false);

        assertThat(published).singleElement().satisfies(changeSet -> {
            assertThat(changeSet.changeSetId()).isEqualTo("change-set-1");
            assertThat(changeSet.changes()).singleElement().satisfies(change -> {
                assertThat(change.type()).isEqualTo(DataChangeTypes.RECORD_UPDATED);
                assertThat(change.moduleAlias()).isEqualTo("iam.employee");
                assertThat(change.recordId()).isEqualTo("employee-1");
            });
        });
    }

    private static final class FirstModule {
    }

    private static final class SecondModule {
    }
}
