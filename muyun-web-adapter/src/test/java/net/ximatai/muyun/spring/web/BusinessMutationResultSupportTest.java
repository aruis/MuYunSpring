package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessMutationResultSupportTest {
    @Test
    void shouldReportBusinessMessageAndUpdatedRecord() {
        MutationContext context = new MutationContext();

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            BusinessMutationResultSupport.successUpdated(
                    "demo.action.done", "业务动作已完成", "demo.module", "record-1");

            assertThat(context.message().code()).isEqualTo("demo.action.done");
            assertThat(context.message().text()).isEqualTo("业务动作已完成");
            assertThat(context.committedChangeSet(Class::getSimpleName).changes())
                    .singleElement()
                    .satisfies(change -> {
                        assertThat(change.type()).isEqualTo("record-updated");
                        assertThat(change.moduleAlias()).isEqualTo("demo.module");
                        assertThat(change.recordId()).isEqualTo("record-1");
                    });
        }
    }

    @Test
    void shouldReportBusinessMessageAndCollectionChange() {
        MutationContext context = new MutationContext();

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            BusinessMutationResultSupport.successCollectionChanged(
                    "demo.collection.changed", "集合已变化", "demo.module");

            assertThat(context.message().code()).isEqualTo("demo.collection.changed");
            assertThat(context.message().text()).isEqualTo("集合已变化");
            assertThat(context.committedChangeSet(Class::getSimpleName).changes())
                    .singleElement()
                    .satisfies(change -> {
                        assertThat(change.type()).isEqualTo("collection-changed");
                        assertThat(change.moduleAlias()).isEqualTo("demo.module");
                    });
        }
    }
}
