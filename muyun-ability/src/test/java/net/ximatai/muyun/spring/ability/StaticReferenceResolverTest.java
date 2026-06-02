package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceProject;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.ability.reference.StaticReferenceResolver;


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
                .isInstanceOf(PlatformException.class)
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

    @Test
    void writeTitleValueShouldWrapFieldTypeMismatch() {
        WrongTitleTypeRecord record = new WrongTitleTypeRecord();

        assertThatThrownBy(() -> StaticReferenceResolver.writeTitleValue(record, "userTitle", List.of("User One")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("Cannot write reference title field")
                .hasMessageContaining("WrongTitleTypeRecord.userTitle")
                .hasMessageContaining("java.util");
    }

    @Test
    void plansShouldRejectTitleOutputWithoutAutoTitle() {
        assertThatThrownBy(() -> StaticReferenceResolver.plans(TitleOutputWithoutAutoTitleRecord.class))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("titleOutputField requires autoTitle")
                .hasMessageContaining("customerId");
    }

    @Test
    void plansShouldRejectDuplicateOutputFields() {
        assertThatThrownBy(() -> StaticReferenceResolver.plans(DuplicateOutputFieldRecord.class))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("duplicate reference outputField")
                .hasMessageContaining("customerId.customerTitle");
    }

    private static final class CollectionReferenceRecord {
        @ReferenceTo(moduleAlias = "iam", entityAlias = "user", cardinality = ReferenceCardinality.MANY)
        private List<String> userIds;
    }

    private static final class WrongTitleTypeRecord {
        private String userTitle;
    }

    private static final class TitleOutputWithoutAutoTitleRecord {
        @ReferenceTo(moduleAlias = "demo", entityAlias = "customer", titleOutputField = "customerTitle")
        private String customerId;
    }

    private static final class DuplicateOutputFieldRecord {
        @ReferenceTo(
                moduleAlias = "demo",
                entityAlias = "customer",
                autoTitle = true,
                titleOutputField = "customerTitle",
                projections = @ReferenceProject(targetField = "status", outputField = "customerTitle")
        )
        private String customerId;
    }
}
