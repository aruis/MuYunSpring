package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import net.ximatai.muyun.spring.ability.reference.ReferenceIntegrity;
import net.ximatai.muyun.spring.ability.reference.ReferenceLoad;
import net.ximatai.muyun.spring.ability.reference.ReferenceHop;
import net.ximatai.muyun.spring.ability.reference.ReferenceTarget;
import net.ximatai.muyun.spring.ability.reference.ReferenceTargetUnavailablePolicy;
import net.ximatai.muyun.spring.ability.reference.ReferenceTo;
import net.ximatai.muyun.spring.ability.reference.StaticReferenceResolver;
import net.ximatai.muyun.spring.ability.reference.ReferencedBy;
import net.ximatai.muyun.spring.ability.reference.StaticReferencedByResolver;


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
                    assertThat(plan.projections()).containsExactlyInAnyOrder(
                            new net.ximatai.muyun.spring.ability.reference.ReferenceProjection("title", "customerTitle"),
                            new net.ximatai.muyun.spring.ability.reference.ReferenceProjection("status", "customerStatus"));
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
    void referencedByShouldCompileInverseReadAssociationFromReferenceFact() {
        assertThat(StaticReferencedByResolver.plans(InverseParent.class))
                .containsExactly(new StaticReferencedByResolver.ReferencedByPlan("records", InverseChild.class, "parentId"));
    }

    @Test
    void referenceLoadShouldCompileTypedTwoHopPath() {
        assertThat(StaticReferenceResolver.loadPaths(TwoHopSource.class))
                .containsExactly(new net.ximatai.muyun.spring.ability.reference.ReferenceLoadPath(
                        "middleId", ReferenceTarget.of("demo", "middle"), List.of(
                                new net.ximatai.muyun.spring.ability.reference.ReferenceLoadPath.Hop(
                                        ReferenceTarget.of("demo", "terminal"), "terminalId")),
                        "title", "terminalTitle"));
    }

    @Test
    void referencedByShouldRequireTransientOutput() {
        assertThatThrownBy(() -> StaticReferencedByResolver.plans(PersistentInverseParent.class))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("transient List<EntityContract>");
    }

    @Test
    void plansShouldResolveClassTargetAndIntegrity() {
        assertThat(StaticReferenceResolver.plans(ClassTargetReferenceRecord.class))
                .singleElement()
                .satisfies(plan -> {
                    assertThat(plan.target()).isEqualTo(ReferenceTarget.of("iam", "organization"));
                    assertThat(plan.integrity().onTargetUnavailable())
                            .isEqualTo(ReferenceTargetUnavailablePolicy.RESTRICT);
                });
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
    void writeLoadedValueShouldWrapFieldTypeMismatch() {
        WrongTitleTypeRecord record = new WrongTitleTypeRecord();

        assertThatThrownBy(() -> StaticReferenceResolver.writeLoadedValue(record, "userTitle", List.of("User One")))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("Cannot write reference title field")
                .hasMessageContaining("WrongTitleTypeRecord.userTitle")
                .hasMessageContaining("java.util");
    }

    @Test
    void plansShouldRejectUnknownLoadSource() {
        assertThatThrownBy(() -> StaticReferenceResolver.plans(UnknownLoadSourceRecord.class))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("ReferenceLoad source must declare @ReferenceTo");
    }

    @Test
    void plansShouldRejectNonTransientLoadOutput() {
        assertThatThrownBy(() -> StaticReferenceResolver.plans(PersistentLoadOutputRecord.class))
                .isInstanceOf(PlatformException.class)
                .hasMessageContaining("ReferenceLoad output must be transient");
    }

    private static final class CollectionReferenceRecord {
        @ReferenceTo(moduleAlias = "iam", entityAlias = "user", cardinality = ReferenceCardinality.MANY)
        private List<String> userIds;
    }

    private static final class InverseParent extends net.ximatai.muyun.spring.common.model.standard.StandardEntity {
        @ReferencedBy
        private transient List<InverseChild> records;
    }

    private static final class InverseChild extends net.ximatai.muyun.spring.common.model.standard.StandardEntity {
        @ReferenceTo(target = InverseParentService.class)
        private String parentId;
    }

    private static final class PersistentInverseParent extends net.ximatai.muyun.spring.common.model.standard.StandardEntity {
        @ReferencedBy
        private List<InverseChild> records;
    }

    public static final class InverseParentService {
        public static final String MODULE_ALIAS = "demo.inverseParent";
    }

    private static final class ClassTargetReferenceRecord {
        @ReferenceTo(target = OrganizationService.class,
                integrity = @ReferenceIntegrity(onTargetUnavailable = ReferenceTargetUnavailablePolicy.RESTRICT))
        private String organizationId;
    }

    public static final class OrganizationService {
        public static final String MODULE_ALIAS = "iam.organization";
    }

    private static final class TwoHopSource extends net.ximatai.muyun.spring.common.model.standard.StandardEntity {
        @ReferenceTo(target = MiddleService.class)
        private String middleId;
        @ReferenceLoad(source = "middleId", hops = @ReferenceHop(target = TerminalService.class, via = "terminalId"))
        private transient String terminalTitle;
    }

    private static final class TwoHopMiddle extends net.ximatai.muyun.spring.common.model.standard.StandardEntity {
        @ReferenceTo(target = TerminalService.class)
        private String terminalId;
    }

    public static final class MiddleService {
        public static final String MODULE_ALIAS = "demo.middle";
    }

    public static final class TerminalService {
        public static final String MODULE_ALIAS = "demo.terminal";
    }

    private static final class WrongTitleTypeRecord {
        private String userTitle;
    }

    private static final class UnknownLoadSourceRecord {
        @ReferenceLoad(source = "customerId", field = "title")
        private transient String customerTitle;
    }

    private static final class PersistentLoadOutputRecord {
        @ReferenceTo(moduleAlias = "demo", entityAlias = "customer")
        private String customerId;
        @ReferenceLoad(source = "customerId", field = "title")
        private String customerTitle;
    }
}
