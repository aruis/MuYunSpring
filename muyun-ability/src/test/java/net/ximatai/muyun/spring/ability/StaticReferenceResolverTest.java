package net.ximatai.muyun.spring.ability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class StaticReferenceResolverTest {
    @Test
    void collectShouldNormalizeSingleAndManyReferenceValues() {
        DemoReferencingRecord record = new DemoReferencingRecord(" customer-1 ", "user-owner");
        record.setWatcherIds("user-1, user-2, user-1, ");

        assertThat(StaticReferenceResolver.collect(record))
                .containsEntry(ReferenceTarget.of("demo", "customer"), java.util.Set.of("customer-1"))
                .containsEntry(ReferenceTarget.of("iam", "user"), java.util.Set.of("user-owner", "user-1", "user-2"));
        assertThat(StaticReferenceResolver.plans(DemoReferencingRecord.class))
                .first()
                .satisfies(plan -> {
                    assertThat(plan.sourceField()).isEqualTo("customerId");
                    assertThat(plan.autoTitle()).isTrue();
                    assertThat(plan.titleOutputField()).isEqualTo("customerTitle");
                });
    }

    @Test
    void collectShouldIgnoreBlankReferenceValues() {
        DemoReferencingRecord record = new DemoReferencingRecord(" ", null);
        record.setWatcherIds(" , ");

        assertThat(StaticReferenceResolver.collect(record)).isEmpty();
    }

    @Test
    void collectShouldRejectSourceTypeMismatch() {
        DemoPlainRecord record = new DemoPlainRecord("plain");

        assertThatThrownBy(() -> StaticReferenceResolver.collect(DemoReferencingRecord.class, record))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("reference source type mismatch")
                .hasMessageContaining(DemoReferencingRecord.class.getName())
                .hasMessageContaining(DemoPlainRecord.class.getName());
    }

    @Test
    void collectShouldSupportCollectionReferenceValues() {
        CollectionReferenceRecord record = new CollectionReferenceRecord();
        record.userIds = List.of("user-1", " user-2 ", "user-1");

        assertThat(StaticReferenceResolver.collect(record))
                .containsEntry(ReferenceTarget.of("iam", "user"), java.util.Set.of("user-1", "user-2"));
    }

    @Test
    void collectResultShouldBeReadOnly() {
        DemoReferencingRecord record = new DemoReferencingRecord("customer-1", "user-owner");
        var references = StaticReferenceResolver.collect(record);

        assertThatThrownBy(() -> references.put(ReferenceTarget.of("demo", "customer"), java.util.Set.of("customer-2")))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> references.get(ReferenceTarget.of("demo", "customer")).add("customer-2"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static final class CollectionReferenceRecord {
        @ReferenceTo(moduleAlias = "iam", entityCode = "user", cardinality = ReferenceCardinality.MANY)
        private List<String> userIds;
    }
}
