package net.ximatai.muyun.spring.dynamic.metadata;

import net.ximatai.muyun.spring.ability.reference.ReferenceCardinality;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssociationViewDefinitionTest {
    private final ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

    @Test
    void shouldAllowRelationAssociationCodeWithUnderscore() {
        ModuleDefinition module = module(List.of(EntityRelationDefinition.child(
                        "invoice_lines", "invoice", "invoice_line", "invoiceId")),
                List.of(),
                List.of(EntityAssociationViewDefinition.childRelation(
                        "invoice_lines", "invoice", "sales.invoice", "invoice_line", "invoice_lines")));

        assertThatCode(() -> validator.validate(module)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectRelationAssociationTargetOutsideCurrentModule() {
        ModuleDefinition module = module(List.of(EntityRelationDefinition.child(
                        "lines", "invoice", "invoice_line", "invoiceId")),
                List.of(),
                List.of(EntityAssociationViewDefinition.childRelation(
                        "lines", "invoice", "other.invoice", "invoice_line", "lines")));

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("target module must be current module");
    }

    @Test
    void shouldRejectAssociationViewWithRelationAndReferenceAtSameTime() {
        ModuleDefinition module = module(List.of(EntityRelationDefinition.child(
                        "lines", "invoice", "invoice_line", "invoiceId")),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", "sales.invoice.invoice")),
                List.of(new EntityAssociationViewDefinition(
                        "invalid",
                        "invoice_line",
                        "sales.invoice",
                        "invoice",
                        AssociationViewDisplayMode.LINKED_RECORD,
                        "lines",
                        "invoiceId",
                        EntityViewType.FORM,
                        false)));

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("exactly one");
    }

    @Test
    void shouldRejectManyReferenceAssociationViewAsSingleRecord() {
        ModuleDefinition module = module(List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", "sales.invoice.invoice").many()),
                List.of(new EntityAssociationViewDefinition(
                        "invoiceId",
                        "invoice_line",
                        "sales.invoice",
                        "invoice",
                        AssociationViewDisplayMode.LINKED_RECORD,
                        null,
                        "invoiceId",
                        EntityViewType.FORM,
                        false)));

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("many reference");
    }

    @Test
    void shouldCreateManyReferenceAssociationAsLinkedList() {
        EntityAssociationViewDefinition view = EntityAssociationViewDefinition.reference(
                "invoiceId", "invoice_line", "sales.invoice", "invoice", "invoiceId", ReferenceCardinality.MANY);

        ModuleDefinition module = module(List.of(),
                List.of(EntityReferenceDefinition.to("invoice_line", "invoiceId", "sales.invoice.invoice").many()),
                List.of(view));

        assertThatCode(() -> validator.validate(module)).doesNotThrowAnyException();
    }

    private ModuleDefinition module(List<EntityRelationDefinition> relations,
                                    List<EntityReferenceDefinition> references,
                                    List<EntityAssociationViewDefinition> associationViews) {
        return new ModuleDefinition(
                "sales.invoice",
                "Invoice",
                List.of(
                        new EntityDefinition("invoice", "sales_invoice", "Invoice", List.of(FieldDefinition.titleField()),
                                java.util.Set.of(EntityCapability.REFERENCE)),
                        new EntityDefinition("invoice_line", "sales_invoice_line", "Invoice line", List.of(
                                FieldDefinition.titleField(),
                                new FieldDefinition("invoiceId", "invoice_id", FieldType.STRING, "Invoice")
                        ), java.util.Set.of(EntityCapability.REFERENCE))
                ),
                relations,
                references,
                List.of(),
                associationViews,
                List.of()
        );
    }
}
