package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessMutationResultSupportTest {
    @Test
    void shouldReportBusinessMessageAndExplicitChange() {
        MutationContext context = new MutationContext();

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            BusinessMutationResultSupport.success("demo.action.done", "业务动作已完成");
            BusinessMutationResultSupport.updated("demo.module", "record-1");

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
}
