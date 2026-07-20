package net.ximatai.muyun.spring.dynamic.metadata;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModuleDefinitionBuilderTest {
    @Test
    void builderCreatesImmutableDefinitionAndDefaultsMainEntity() {
        List<EntityDefinition> entities = new ArrayList<>(List.of(
                new EntityDefinition("contract", "sales_contract", "Contract", List.of())));

        ModuleDefinition definition = ModuleDefinition.builder("sales.contract", "Contract")
                .entities(entities)
                .build();
        entities.clear();

        assertThat(definition.entities()).hasSize(1);
        assertThat(definition.mainEntityAlias()).isEqualTo("contract");
        assertThat(definition.relations()).isEmpty();
    }

    @Test
    void toBuilderPreservesDefinitionAndSupportsNamedChanges() {
        ModuleDefinition original = new ModuleDefinition("sales.contract", "Contract", List.of());

        ModuleDefinition changed = original.toBuilder().mainEntityAlias("contract").build();

        assertThat(changed.moduleAlias()).isEqualTo(original.moduleAlias());
        assertThat(changed.name()).isEqualTo(original.name());
        assertThat(changed.mainEntityAlias()).isEqualTo("contract");
    }
}
