package net.ximatai.muyun.spring.ability;

import net.ximatai.muyun.spring.ability.child.ChildPlan;
import net.ximatai.muyun.spring.ability.child.ChildRef;
import net.ximatai.muyun.spring.ability.child.StaticChildResolver;
import net.ximatai.muyun.spring.ability.child.StaticChildResolverTestAccess;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StaticChildResolverTest {
    @BeforeEach
    void setUp() {
        StaticChildResolverTestAccess.clearCacheForTests();
    }

    @Test
    void plansShouldCompileChildRefAnnotation() {
        assertThat(StaticChildResolver.plans(DemoInvoice.class))
                .containsExactly(
                        new ChildPlan("lines", "invoice", "invoiceLine", "invoiceId", true, true),
                        new ChildPlan("notes", "invoice", "invoiceNote", "invoiceId", true, true)
                );
        assertThat(StaticChildResolver.plan(DemoInvoice.class, "notes"))
                .isEqualTo(new ChildPlan("notes", "invoice", "invoiceNote", "invoiceId", true, true));
    }

    @Test
    void plansShouldDefaultEntityCodesWhenAnnotationOmitsThem() {
        assertThat(StaticChildResolver.plans(DefaultChildParent.class))
                .containsExactly(new ChildPlan(
                        "items",
                        "defaultChildParent",
                        "defaultChildItem",
                        "parentId",
                        true,
                        false
                ));
    }

    @Test
    void ruleShouldReadPopulateAndWriteDeclaredChildFields() {
        StaticChildResolver.ChildRule rule = StaticChildResolver.rule(DemoInvoice.class, "lines");
        DemoInvoice invoice = new DemoInvoice("Invoice", List.of());
        DemoInvoiceLine line = new DemoInvoiceLine("First line");

        rule.setParentId(line, "invoice-1");
        rule.populate(invoice, List.of(line));

        assertThat(line.getInvoiceId()).isEqualTo("invoice-1");
        assertThat(rule.<DemoInvoice, DemoInvoiceLine>children(invoice))
                .containsExactly(line);
    }

    @Test
    void singlePlanShouldRejectMultipleChildRelations() {
        assertThatThrownBy(() -> StaticChildResolver.singlePlan(DemoInvoice.class))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("expected exactly one child relation plan")
                .hasMessageContaining("lines")
                .hasMessageContaining("notes")
                .hasMessageContaining("childRelation(relationCode");
    }

    @Test
    void singlePlanShouldRejectMissingChildRelations() {
        assertThatThrownBy(() -> StaticChildResolver.singlePlan(NoChildRecord.class))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("expected exactly one child relation plan")
                .hasMessageContaining("actual relationCodes: []")
                .hasMessageContaining("@ChildRef");
    }

    @Test
    void singlePlanShouldRejectMissingParentModelClass() {
        assertThatThrownBy(() -> StaticChildResolver.singlePlan(null))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("parentModelClass");
    }

    @Test
    void planShouldRejectUnknownRelationCode() {
        assertThatThrownBy(() -> StaticChildResolver.plan(DemoInvoice.class, "missing"))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("unknown child relationCode");
    }

    @Test
    void planShouldRejectMissingParentModelClass() {
        assertThatThrownBy(() -> StaticChildResolver.plan(null, "lines"))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("parentModelClass");
    }

    @Test
    void rulesShouldRejectNonListChildField() {
        assertThatThrownBy(() -> StaticChildResolver.plans(InvalidChildFieldRecord.class))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("must be List");
    }

    @Test
    void rulesShouldRejectMismatchedChildGeneric() {
        assertThatThrownBy(() -> StaticChildResolver.plans(MismatchedChildFieldRecord.class))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("not assignable");
    }

    @Test
    void rulesShouldRejectDuplicateRelationCodes() {
        assertThatThrownBy(() -> StaticChildResolver.plans(DuplicateRelationCodeRecord.class))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("duplicate child relationCode");
    }

    @Test
    void rulesShouldRejectMissingChildForeignKeyField() {
        assertThatThrownBy(() -> StaticChildResolver.plans(MissingForeignKeyRecord.class))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("cannot find child foreign key field")
                .hasMessageContaining(MissingForeignKeyChild.class.getName())
                .hasMessageContaining("invoiceId");
    }

    @Test
    void rulesShouldRejectNonStringChildForeignKeyField() {
        assertThatThrownBy(() -> StaticChildResolver.plans(NonStringForeignKeyRecord.class))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("childForeignKeyField must be String")
                .hasMessageContaining(NonStringForeignKeyChild.class.getName())
                .hasMessageContaining("invoiceId");
    }

    private static final class InvalidChildFieldRecord extends StandardEntity {
        @ChildRef(childModel = DemoInvoiceLine.class, childForeignKeyField = "invoiceId")
        private DemoInvoiceLine line;
    }

    private static final class MismatchedChildFieldRecord extends StandardEntity {
        @ChildRef(childModel = DemoInvoiceLine.class, childForeignKeyField = "invoiceId")
        private List<DemoPlainRecord> lines;
    }

    private static final class DuplicateRelationCodeRecord extends StandardEntity {
        @ChildRef(relationCode = "lines", childModel = DemoInvoiceLine.class, childForeignKeyField = "invoiceId")
        private List<DemoInvoiceLine> firstLines;

        @ChildRef(relationCode = "lines", childModel = DemoInvoiceLine.class, childForeignKeyField = "invoiceId")
        private List<DemoInvoiceLine> secondLines;
    }

    private static final class MissingForeignKeyRecord extends StandardEntity {
        @ChildRef(childModel = MissingForeignKeyChild.class, childForeignKeyField = "invoiceId")
        private List<MissingForeignKeyChild> lines;
    }

    private static final class MissingForeignKeyChild extends StandardEntity {
    }

    private static final class NonStringForeignKeyRecord extends StandardEntity {
        @ChildRef(childModel = NonStringForeignKeyChild.class, childForeignKeyField = "invoiceId")
        private List<NonStringForeignKeyChild> lines;
    }

    private static final class NonStringForeignKeyChild extends StandardEntity {
        private Integer invoiceId;
    }

    private static final class NoChildRecord extends StandardEntity {
    }

    private static final class DefaultChildParent extends StandardEntity {
        @ChildRef(childModel = DefaultChildItem.class, childForeignKeyField = "parentId")
        private List<DefaultChildItem> items;
    }

    private static final class DefaultChildItem extends StandardEntity {
        private String parentId;
    }
}
