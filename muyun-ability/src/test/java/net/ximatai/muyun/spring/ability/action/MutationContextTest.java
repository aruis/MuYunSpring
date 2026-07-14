package net.ximatai.muyun.spring.ability.action;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MutationContextTest {
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

    private static final class FirstModule {
    }

    private static final class SecondModule {
    }
}
