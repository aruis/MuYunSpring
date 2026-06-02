package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionKind;
import net.ximatai.muyun.spring.dynamic.metadata.EntityAssociationViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityReferenceDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewFieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityViewType;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinitionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicModuleDescriptorRuntimeTest {
    @Test
    void shouldDescribePublishedModuleFromRegistryAndRuntime() {
        ModuleDefinition module = new ModuleDefinition("sales.contract", "Contract", List.of(
                new EntityDefinition("contract", "sales_contract", "Contract",
                        List.of(FieldDefinition.titleField()), Set.of(EntityCapability.REFERENCE))
        ));
        DynamicModuleRegistry registry = new DynamicModuleRegistry();
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(nullOperations(), registry).publish(module);

        DynamicModuleDescriptor fromRegistry = registry.describe("sales.contract");
        DynamicModuleDescriptor fromRuntime = runtime.describe("sales.contract");

        assertThat(fromRegistry).isEqualTo(fromRuntime);
        assertThat(fromRuntime.entities().getFirst().fields().getFirst().titleField()).isTrue();
    }

    @Test
    void shouldExposeStableRuntimeApiForDescriptorViewActionAndReferenceContracts() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(
                        new EntityDefinition("contract", "sales_contract", "Contract",
                                List.of(FieldDefinition.titleField()), Set.of(EntityCapability.REFERENCE)),
                        new EntityDefinition("line", "sales_contract_line", "Line",
                                List.of(FieldDefinition.titleField(), new FieldDefinition("contractId", "contract_id",
                                        net.ximatai.muyun.spring.dynamic.metadata.FieldType.STRING, "Contract")),
                                Set.of(EntityCapability.REFERENCE))
                ),
                List.of(EntityRelationDefinition.child("lines", "contract", "line", "contractId")),
                List.of(EntityReferenceDefinition.to("line", "contractId", "sales.contract.contract")),
                List.of(new EntityViewDefinition("contract", EntityViewType.FORM, "Contract form",
                        List.of(new EntityViewFieldDefinition("title")))),
                List.of(
                        EntityAssociationViewDefinition.childRelation("lines", "contract", "sales.contract",
                                "line", "lines"),
                        EntityAssociationViewDefinition.reference("contractId", "line", "sales.contract",
                                "contract", "contractId")
                ),
                List.of(
                        new EntityActionDefinition("contract", "create", EntityActionKind.RECORD,
                                "Create contract", true, null),
                        new EntityActionDefinition("line", "exportLine", EntityActionKind.CUSTOM,
                                "Export line", true, null)
                )
        );
        DynamicRecordRuntime runtime = new DynamicRecordRuntime(nullOperations()).publish(module);
        DynamicRecordService service = new DynamicRecordService(runtime);

        DynamicRecordService.ModuleOperations moduleApi = service.module("sales.contract");
        DynamicRecordService.EntityOperations contractApi = moduleApi.entity("contract");

        assertThat(moduleApi.describe().moduleAlias()).isEqualTo("sales.contract");
        assertThat(moduleApi.actions()).extracting(action -> action.code()).contains("create");
        assertThat(moduleApi.actions()).extracting(action -> action.code()).doesNotContain("exportLine");
        assertThat(moduleApi.action("create").actionAuth()).isTrue();
        assertThatThrownBy(() -> moduleApi.action("exportLine"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("sales.contract.exportLine");
        assertThat(moduleApi.relations()).extracting(relation -> relation.code()).containsExactly("lines");
        assertThat(moduleApi.references()).extracting(reference -> reference.sourceField()).containsExactly("contractId");
        assertThat(moduleApi.associationViews()).extracting(view -> view.code()).containsExactly("lines", "contractId");
        assertThat(contractApi.describe().entityCode()).isEqualTo("contract");
        assertThat(contractApi.action("create").actionAuth()).isTrue();
        assertThat(contractApi.view(EntityViewType.FORM).title()).isEqualTo("Contract form");
        assertThat(contractApi.associationView("lines").targetEntity()).isEqualTo("line");
        DynamicRecordService.EntityOperations lineApi = service.entity("sales.contract", "line");
        assertThat(lineApi.actions()).extracting(action -> action.code()).contains("exportLine");
        assertThat(lineApi.references()).extracting(reference -> reference.sourceField()).containsExactly("contractId");
        assertThat(lineApi.reference("contractId").targetEntityCode()).isEqualTo("contract");
        assertThat(lineApi.associationView("contractId").targetEntity()).isEqualTo("contract");
    }

    @Test
    void shouldRejectUnknownRuntimeApiContractNames() {
        ModuleDefinition module = new ModuleDefinition("sales.contract", "Contract", List.of(
                new EntityDefinition("contract", "sales_contract", "Contract",
                        List.of(FieldDefinition.titleField()), Set.of(EntityCapability.REFERENCE))
        ));
        DynamicRecordService service = new DynamicRecordService(new DynamicRecordRuntime(nullOperations()).publish(module));

        assertThatThrownBy(() -> service.entityDescriptor("sales.contract", "missing"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown dynamic entity");
        assertThatThrownBy(() -> service.entity("sales.contract", "contract").action("missing"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("sales.contract.contract.missing");
        assertThatThrownBy(() -> service.module("sales.contract").action("missing"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("sales.contract.missing");
        assertThatThrownBy(() -> service.entity("sales.contract", "contract").reference("missing"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("sales.contract.contract.missing");
        assertThatThrownBy(() -> service.entity("sales.contract", "contract").associationView("missing"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("sales.contract.contract.missing");
    }

    @SuppressWarnings("unchecked")
    private net.ximatai.muyun.database.core.IDatabaseOperations<Object> nullOperations() {
        return org.mockito.Mockito.mock(net.ximatai.muyun.database.core.IDatabaseOperations.class);
    }
}
