package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StandardMutationResultSupportTest {
    @Test
    void shouldReportStandardSortMessageAndCollectionChange() {
        MutationContext context = new MutationContext();

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            int count = StandardMutationResultSupport.sorted("demo.module", () -> 1);

            assertThat(count).isEqualTo(1);
            assertThat(context.message().code()).isEqualTo("platform.crud.sorted");
            assertThat(context.message().text()).isEqualTo("排序成功");
            CommittedChangeSet changeSet = context.committedChangeSet(Class::getSimpleName);
            assertThat(changeSet.changes()).singleElement().satisfies(change -> {
                assertThat(change.moduleAlias()).isEqualTo("demo.module");
                assertThat(change.type()).isEqualTo("collection-changed");
            });
        }
    }

    @Test
    void shouldSkipCountMutationResultWhenNothingChanged() {
        MutationContext context = new MutationContext();

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            int count = StandardMutationResultSupport.deleted("demo.module", "record-1", () -> 0);

            assertThat(count).isZero();
            assertThat(context.message()).isNull();
            assertThat(context.committedChangeSet(Class::getSimpleName).changes()).isEmpty();
        }
    }
}
