package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.common.platform.EntityCapability;
import net.ximatai.muyun.spring.platform.module.ModuleEntryType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StaticModuleDefinitionBuilderTest {
    @Test
    void builderAppliesStableDefaultsAndCapabilityImplications() {
        StaticModuleDefinition definition = StaticModuleDefinition
                .builder("sales", "sales.contract", " Contract ")
                .capabilities(Set.of(EntityCapability.APPROVAL))
                .build();

        assertThat(definition.title()).isEqualTo("Contract");
        assertThat(definition.entryType()).isEqualTo(ModuleEntryType.MODULE);
        assertThat(definition.capabilities())
                .containsExactlyInAnyOrder(EntityCapability.APPROVAL, EntityCapability.WORKFLOW);
        assertThat(definition.actions()).isEmpty();
    }

    @Test
    void toBuilderPreservesDefinitionAndSupportsNamedChanges() {
        StaticModuleDefinition original = StaticModuleDefinition
                .builder("sales", "sales.contract", "Contract")
                .build();

        StaticModuleDefinition changed = original.toBuilder()
                .parentModuleAlias("sales.root")
                .build();

        assertThat(changed.applicationAlias()).isEqualTo(original.applicationAlias());
        assertThat(changed.moduleAlias()).isEqualTo(original.moduleAlias());
        assertThat(changed.parentModuleAlias()).isEqualTo("sales.root");
    }
}
