package net.ximatai.muyun.spring.web;

import net.ximatai.muyun.spring.ability.action.CommittedChangeSet;
import net.ximatai.muyun.spring.ability.action.MutationContext;
import net.ximatai.muyun.spring.ability.action.MutationContextHolder;
import net.ximatai.muyun.spring.ability.security.FieldProtectionAbility;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.common.model.title.TitleField;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldIncludeRecordLabelInStandardMutationSuccessMessage() {
        MutationContext context = new MutationContext();

        try (MutationContextHolder.Scope ignored = MutationContextHolder.use(context)) {
            int count = StandardMutationResultSupport.deleted(new TestScopedWeb(), "record-1", "演示租户", () -> 1);

            assertThat(count).isEqualTo(1);
            assertThat(context.message().text()).isEqualTo("「演示租户」删除成功");
        }
    }

    @Test
    void shouldResolveRecordLabelThroughFieldOutputProtection() {
        @SuppressWarnings("unchecked")
        FieldProtectionAbility<MaskedRecord> service = mock(FieldProtectionAbility.class);
        when(service.maskProtectedValue("displayName", "敏感名称", FieldOutputContext.VIEW))
                .thenReturn("敏***称");
        MaskedRecord record = new MaskedRecord();
        record.displayName = "敏感名称";

        String label = new TestRecordLabelWeb(service).recordLabel(record);

        assertThat(label).isEqualTo("敏***称");
        verify(service).maskProtectedValue("displayName", "敏感名称", FieldOutputContext.VIEW);
    }

    private static final class TestScopedWeb implements ScopedWeb<net.ximatai.muyun.spring.ability.CrudAbility<net.ximatai.muyun.spring.common.model.contract.EntityContract>> {
        @Override
        public net.ximatai.muyun.spring.ability.CrudAbility<net.ximatai.muyun.spring.common.model.contract.EntityContract> service() {
            return null;
        }

        @Override
        public String webScopeName() {
            return "demo.module";
        }
    }

    private record TestRecordLabelWeb(FieldProtectionAbility<MaskedRecord> service)
            implements RecordLabelWeb<MaskedRecord> {
    }

    private static final class MaskedRecord extends StandardEntity {
        @TitleField
        private String displayName;
    }
}
