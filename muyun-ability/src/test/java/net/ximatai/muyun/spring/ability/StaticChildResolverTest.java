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
    void singlePlanShouldRejectMultipleChildRelations() {
        assertThatThrownBy(() -> StaticChildResolver.singlePlan(DemoInvoice.class))
                .isInstanceOf(AbilityException.class)
                .hasMessageContaining("expected exactly one child relation plan")
                .hasMessageContaining("lines")
                .hasMessageContaining("notes")
                .hasMessageContaining("childRelation(relationCode");
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

    private static final class InvalidChildFieldRecord extends StandardEntity {
        @ChildRef(parentEntity = "invoice", childModel = DemoInvoiceLine.class, childForeignKeyField = "invoiceId")
        private DemoInvoiceLine line;
    }

    private static final class MismatchedChildFieldRecord extends StandardEntity {
        @ChildRef(parentEntity = "invoice", childModel = DemoInvoiceLine.class, childForeignKeyField = "invoiceId")
        private List<DemoPlainRecord> lines;
    }

    private static final class DuplicateRelationCodeRecord extends StandardEntity {
        @ChildRef(relationCode = "lines", parentEntity = "invoice", childModel = DemoInvoiceLine.class, childForeignKeyField = "invoiceId")
        private List<DemoInvoiceLine> firstLines;

        @ChildRef(relationCode = "lines", parentEntity = "invoice", childModel = DemoInvoiceLine.class, childForeignKeyField = "invoiceId")
        private List<DemoInvoiceLine> secondLines;
    }
}
