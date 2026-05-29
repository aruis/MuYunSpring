package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.common.option.OptionBinding;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
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
                                FieldDefinition.titleField(),
                                FieldDefinition.string("status", "Status").dictionary("crm", "customer_status")
                        ), Set.of(EntityCapability.CRUD, EntityCapability.REFERENCE)),
                        new EntityDefinition("contact", "crm_contact", "Contact", List.of(
                                FieldDefinition.titleField(),
                                FieldDefinition.string("customerId", "Customer")
                        ), Set.of(EntityCapability.CRUD))
                ),
                List.of(EntityRelationDefinition.child("contacts", "customer", "contact", "customerId")
                        .withAutoPopulate()),
                List.of(EntityReferenceDefinition.to("contact", "customerId", "crm.customer.customer")
                        .withAutoTitle("customerTitle")
                        .withProjection("title", "customerTitle"))
        );

        DynamicModuleDescriptor descriptor = DynamicModuleDescriptor.from(module);

        assertThat(descriptor.moduleAlias()).isEqualTo("crm.customer");
        assertThat(descriptor.entities()).extracting(DynamicEntityDescriptor::entityCode)
                .containsExactly("customer", "contact");
        assertThat(descriptor.entities().getFirst().capabilities()).contains("CRUD", "REFERENCE");
        DynamicFieldDescriptor status = descriptor.entities().getFirst().fields().get(1);
        assertThat(status.fieldName()).isEqualTo("status");
        assertThat(status.optionBinding()).isEqualTo(OptionBinding.dictionary("crm", "customer_status"));
        assertThat(descriptor.relations().getFirst().code()).isEqualTo("contacts");
        assertThat(descriptor.relations().getFirst().autoPopulate()).isTrue();
        DynamicReferenceDescriptor reference = descriptor.references().getFirst();
        assertThat(reference.sourceEntity()).isEqualTo("contact");
        assertThat(reference.targetModuleAlias()).isEqualTo("crm.customer");
        assertThat(reference.targetEntityCode()).isEqualTo("customer");
        assertThat(reference.titleOutputField()).isEqualTo("customerTitle");
        assertThat(reference.projections())
                .containsExactly(new DynamicReferenceProjectionDescriptor("title", "customerTitle"));
    }
}
