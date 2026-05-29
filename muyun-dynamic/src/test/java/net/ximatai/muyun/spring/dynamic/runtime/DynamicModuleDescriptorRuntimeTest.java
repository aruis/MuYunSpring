package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.dynamic.descriptor.DynamicModuleDescriptor;
import net.ximatai.muyun.spring.dynamic.metadata.EntityCapability;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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

    @SuppressWarnings("unchecked")
    private net.ximatai.muyun.database.core.IDatabaseOperations<Object> nullOperations() {
        return org.mockito.Mockito.mock(net.ximatai.muyun.database.core.IDatabaseOperations.class);
    }
}
