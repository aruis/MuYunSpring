package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.common.formula.FormulaRuleKind;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.AssociationViewDisplayMode;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionLevel;
import net.ximatai.muyun.spring.dynamic.metadata.EntityAssociationViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityFormulaRuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewFieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ViewControlType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicModuleDescriptorTest {
    @Test
    void shouldExposeRuntimeModuleDefinitionAsStableDescriptor() {
        ModuleDefinition module = new ModuleDefinition(
                "crm.customer",
                "Customer",
                List.of(
                        new EntityDefinition("customer", "crm_customer", "Customer", List.of(
                                FieldDefinition.titleField().queryable(),
                        FieldDefinition.string("status", "Status")
                                        .dictionary("crm", "customer_status")
                                        .defaultValue("active")
                                        .validationRegex("[a-z_]+")
                                        .notCopyable()
                        ), Set.of(EntityCapability.CRUD, EntityCapability.REFERENCE))
                                .withFormulaRules(
                                        EntityFormulaRuleDefinition
                                                .calculation("lateRule", "title", "{title}")
                                                .sortOrder(20),
                                        EntityFormulaRuleDefinition
                                                .calculation("statusTitle", "title", "{status} + '-' + {title}")
                                                .phase(FormulaRulePhase.DEFAULT_VALUE)
                                                .sortOrder(10)
                                ),
                        new EntityDefinition("contact", "crm_contact", "Contact", List.of(
                                FieldDefinition.titleField(),
                                FieldDefinition.string("customerId", "Customer")
                        ), Set.of(EntityCapability.CRUD))
                ),
                List.of(EntityRelationDefinition.child("contacts", "customer", "contact", "customerId")
                        .withAutoPopulate()),
                List.of(EntityReferenceDefinition.to("contact", "customerId", "crm.customer.customer")
                        .withAutoTitle("customerTitle")
                        .withProjection("title", "customerTitle")),
                List.of(),
                List.of(
                        EntityAssociationViewDefinition.childRelation("contacts", "customer", "crm.customer",
                                "contact", "contacts"),
                        EntityAssociationViewDefinition.reference("customerId", "contact", "crm.customer",
                                "customer", "customerId")
                ),
                List.of()
        );

        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(module);

        assertThat(descriptor.moduleAlias()).isEqualTo("crm.customer");
        assertThat(descriptor.actions())
                .extracting(DynamicActionDescriptor::code)
                .contains("create", "select", "update", "delete", "list", "page", "count",
                        "queryCriteria", "title", "titles", "projections", "referenceOptions");
        assertThat(descriptor.entities()).extracting(DynamicEntityDescriptor::entityCode)
                .containsExactly("customer", "contact");
        assertThat(descriptor.entities().getFirst().capabilities()).contains("CRUD", "REFERENCE");
        assertThat(descriptor.entities().getFirst().formulaRules().getFirst())
                .satisfies(rule -> {
                    assertThat(rule.code()).isEqualTo("statusTitle");
                    assertThat(rule.kind()).isEqualTo(FormulaRuleKind.CALCULATION);
                    assertThat(rule.phase()).isEqualTo(FormulaRulePhase.DEFAULT_VALUE);
                    assertThat(rule.targetField()).isEqualTo("title");
                    assertThat(rule.sortOrder()).isEqualTo(10);
                });
        assertThat(descriptor.entities().getFirst().actions())
                .extracting(DynamicActionDescriptor::code)
                .contains("create", "select", "update", "delete", "list", "page", "count",
                        "queryCriteria", "title", "titles", "projections", "referenceOptions");
        assertThat(descriptor.entities().getFirst().actions().stream()
                .filter(action -> action.code().equals("create"))
                .findFirst())
                .get()
                .extracting(DynamicActionDescriptor::permissionCode)
                .isEqualTo("crm.customer.customer.create");
        DynamicFieldDescriptor status = descriptor.entities().getFirst().fields().get(1);
        assertThat(status.fieldName()).isEqualTo("status");
        assertThat(status.optionBinding()).isEqualTo(OptionBinding.dictionary("crm", "customer_status"));
        assertThat(status.defaultValue()).isEqualTo("active");
        assertThat(status.validationRegex()).isEqualTo("[a-z_]+");
        assertThat(status.copyable()).isFalse();
        assertThat(status.writeProtected()).isFalse();
        DynamicViewDescriptor listView = descriptor.entities().getFirst().views().getFirst();
        assertThat(listView.viewType()).isEqualTo(EntityViewType.LIST);
        assertThat(listView.fields())
                .extracting(DynamicViewFieldDescriptor::fieldName)
                .containsExactly("title", "status");
        assertThat(listView.fields().get(1).controlType()).isEqualTo(ViewControlType.SELECT);
        assertThat(descriptor.entities().getFirst().fields().getFirst().query().defaultOperator())
                .isEqualTo(DynamicQueryOperator.LIKE.name());
        assertThat(descriptor.entities().getFirst().fields().getFirst().query().operators())
                .containsExactly("EQ", "LIKE", "IN");
        assertThat(descriptor.relations().getFirst().code()).isEqualTo("contacts");
        assertThat(descriptor.relations().getFirst().autoPopulate()).isTrue();
        assertThat(descriptor.associationViews())
                .extracting(DynamicAssociationViewDescriptor::code)
                .containsExactly("contacts", "customerId");
        assertThat(descriptor.entities().getFirst().associationViews().getFirst())
                .satisfies(view -> {
                    assertThat(view.displayMode()).isEqualTo(AssociationViewDisplayMode.INLINE_LIST);
                    assertThat(view.targetEntity()).isEqualTo("contact");
                    assertThat(view.relationCode()).isEqualTo("contacts");
                    assertThat(view.queryable()).isTrue();
                });
        assertThat(descriptor.entities().get(1).associationViews().getFirst())
                .satisfies(view -> {
                    assertThat(view.displayMode()).isEqualTo(AssociationViewDisplayMode.LINKED_RECORD);
                    assertThat(view.targetEntity()).isEqualTo("customer");
                    assertThat(view.referenceField()).isEqualTo("customerId");
                    assertThat(view.viewType()).isEqualTo(EntityViewType.FORM);
                    assertThat(view.queryable()).isFalse();
                });
        DynamicReferenceDescriptor reference = descriptor.references().getFirst();
        assertThat(reference.sourceEntity()).isEqualTo("contact");
        assertThat(reference.targetModuleAlias()).isEqualTo("crm.customer");
        assertThat(reference.targetEntityCode()).isEqualTo("customer");
        assertThat(reference.titleOutputField()).isEqualTo("customerTitle");
        assertThat(reference.projections())
                .containsExactly(new DynamicReferenceProjectionDescriptor("title", "customerTitle"));
    }

    @Test
    void shouldExposeMainEntityActionsAsModuleActions() {
        ModuleDefinition module = new ModuleDefinition(
                "crm.customer",
                "Customer",
                List.of(
                        new EntityDefinition("contact", "crm_contact", "Contact",
                                List.of(FieldDefinition.titleField())),
                        new EntityDefinition("customer", "crm_customer", "Customer",
                                List.of(FieldDefinition.titleField()))
                ),
                List.of(EntityRelationDefinition.child("contacts", "customer", "contact", "customerId")),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new EntityActionDefinition("customer", "create", EntityActionKind.RECORD,
                                "新建客户", true, EntityActionLevel.PRIMARY, "crm.customer.create"),
                        new EntityActionDefinition("contact", "exportContact", EntityActionKind.CUSTOM,
                                "导出联系人", true, EntityActionLevel.NORMAL, "crm.contact.export")
                ),
                "customer"
        );

        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(module);

        assertThat(descriptor.actions())
                .extracting(DynamicActionDescriptor::code)
                .contains("create")
                .doesNotContain("exportContact");
        assertThat(descriptor.actions().stream().filter(action -> action.code().equals("create")).findFirst())
                .get()
                .extracting(DynamicActionDescriptor::title)
                .isEqualTo("新建客户");
        assertThat(descriptor.entities().getFirst().actions())
                .extracting(DynamicActionDescriptor::code)
                .contains("exportContact");
    }

    @Test
    void shouldExposeCapabilitySpecificStandardActions() {
        ModuleDefinition module = new ModuleDefinition(
                "crm.customer",
                "Customer",
                List.of(
                        new EntityDefinition("customer", "crm_customer", "Customer", List.of(
                                FieldDefinition.titleField(),
                                FieldDefinition.string("parentId", "Parent"),
                                FieldDefinition.integer("sortOrder", "Sort"),
                                FieldDefinition.bool("enabled", "Enabled")
                        ), Set.of(EntityCapability.TREE, EntityCapability.ENABLE))
                )
        );

        DynamicEntityDescriptor entity = DynamicModuleDescriptor.from(module).entities().getFirst();

        assertThat(entity.actions())
                .extracting(DynamicActionDescriptor::code)
                .contains("children", "ancestorIds", "descendantIds",
                        "sortedList", "reorder", "moveBefore", "moveAfter",
                        "enable", "disable", "isEnabled", "enabledCriteria");
        assertThat(entity.actions().stream()
                .filter(action -> action.code().equals("children"))
                .findFirst())
                .get()
                .extracting(DynamicActionDescriptor::kind)
                .isEqualTo(DynamicActionKind.TREE);
        assertThat(entity.actions().stream()
                .filter(action -> action.code().equals("delete"))
                .findFirst())
                .get()
                .extracting(DynamicActionDescriptor::level)
                .isEqualTo(EntityActionLevel.DANGER);
    }

    @Test
    void shouldApplyConfiguredActionGovernance() {
        ModuleDefinition module = new ModuleDefinition(
                "crm.customer",
                "Customer",
                List.of(
                        new EntityDefinition("customer", "crm_customer", "Customer",
                                List.of(FieldDefinition.titleField()))
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        new EntityActionDefinition("customer", "create", EntityActionKind.RECORD,
                                "新建客户", true, EntityActionLevel.PRIMARY, "crm.customer.create"),
                        new EntityActionDefinition("customer", "delete", EntityActionKind.RECORD,
                                "删除客户", false, EntityActionLevel.DANGER, null),
                        new EntityActionDefinition("customer", "exportData", EntityActionKind.CUSTOM,
                                "导出", true, EntityActionLevel.NORMAL, "crm.customer.export")
                )
        );

        List<DynamicActionDescriptor> actions = DynamicModuleDescriptor.from(module).entities().getFirst().actions();

        assertThat(actions.stream().filter(action -> action.code().equals("create")).findFirst())
                .get()
                .satisfies(action -> {
                    assertThat(action.title()).isEqualTo("新建客户");
                    assertThat(action.enabled()).isTrue();
                    assertThat(action.level()).isEqualTo(EntityActionLevel.PRIMARY);
                    assertThat(action.permissionCode()).isEqualTo("crm.customer.create");
                });
        assertThat(actions.stream().filter(action -> action.code().equals("delete")).findFirst())
                .get()
                .extracting(DynamicActionDescriptor::enabled)
                .isEqualTo(false);
        assertThat(actions.stream().filter(action -> action.code().equals("exportData")).findFirst())
                .get()
                .satisfies(action -> {
                    assertThat(action.kind()).isEqualTo(DynamicActionKind.CUSTOM);
                    assertThat(action.permissionCode()).isEqualTo("crm.customer.export");
                });
    }

    @Test
    void shouldExposeExplicitViewDefinition() {
        ModuleDefinition module = new ModuleDefinition(
                "crm.customer",
                "Customer",
                List.of(
                        new EntityDefinition("customer", "crm_customer", "Customer", List.of(
                                FieldDefinition.titleField(),
                                FieldDefinition.string("status", "Status").dictionary("crm", "customer_status"),
                                FieldDefinition.text("description", "Description")
                        ))
                ),
                List.of(),
                List.of(),
                List.of(new EntityViewDefinition(
                        "customer",
                        EntityViewType.FORM,
                        "Customer form",
                        List.of(
                                new EntityViewFieldDefinition("title").title("Customer name"),
                                new EntityViewFieldDefinition("status").control(ViewControlType.SELECT).readOnly(true),
                                new EntityViewFieldDefinition("description").hidden().control(ViewControlType.TEXTAREA)
                        )
                ))
        );

        List<DynamicViewDescriptor> views = DynamicModuleDescriptor.from(module).entities().getFirst().views();
        DynamicViewDescriptor listView = views.getFirst();
        DynamicViewDescriptor formView = views.get(1);

        assertThat(listView.viewType()).isEqualTo(EntityViewType.LIST);
        assertThat(listView.fields())
                .extracting(DynamicViewFieldDescriptor::fieldName)
                .containsExactly("title", "status", "description");
        assertThat(formView.viewType()).isEqualTo(EntityViewType.FORM);
        assertThat(formView.title()).isEqualTo("Customer form");
        assertThat(formView.fields())
                .extracting(DynamicViewFieldDescriptor::fieldName)
                .containsExactly("title", "status", "description");
        assertThat(formView.fields().getFirst().title()).isEqualTo("Customer name");
        assertThat(formView.fields().get(1).controlType()).isEqualTo(ViewControlType.SELECT);
        assertThat(formView.fields().get(1).readOnly()).isTrue();
        assertThat(formView.fields().get(2).visible()).isFalse();
    }

    @Test
    void shouldNeverRelaxModelRequiredFieldInViewDescriptor() {
        ModuleDefinition module = new ModuleDefinition(
                "crm.customer",
                "Customer",
                List.of(
                        new EntityDefinition("customer", "crm_customer", "Customer", List.of(
                                FieldDefinition.string("code", "Code").required(),
                                FieldDefinition.string("remark", "Remark")
                        ))
                ),
                List.of(),
                List.of(),
                List.of(new EntityViewDefinition(
                        "customer",
                        EntityViewType.FORM,
                        "Customer form",
                        List.of(
                                new EntityViewFieldDefinition("code").required(false),
                                new EntityViewFieldDefinition("remark").required(true)
                        )
                ))
        );

        DynamicViewDescriptor formView = DynamicModuleDescriptor.from(module).entities().getFirst().views().get(1);

        assertThat(formView.fields().getFirst().required()).isTrue();
        assertThat(formView.fields().get(1).required()).isTrue();
    }
}
