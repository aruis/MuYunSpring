package net.ximatai.muyun.spring.module.runtime;

import net.ximatai.muyun.spring.module.metadata.EntityDefinition;
import net.ximatai.muyun.spring.module.metadata.FieldDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinition;
import net.ximatai.muyun.spring.module.metadata.ModuleDefinitionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicModuleRegistryTest {
    @Test
    void shouldRegisterAndResolveDynamicModuleDefinitions() {
        DynamicModuleRegistry registry = new DynamicModuleRegistry();
        ModuleDefinition module = contractModule();

        registry.register(module);

        assertThat(registry.modules()).containsExactly(module);
        assertThat(registry.findModule("sales.contract")).contains(module);
        assertThat(registry.requireEntity("sales.contract", "contract"))
                .isEqualTo(module.entities().getFirst());
    }

    @Test
    void shouldRejectDuplicateOrUnknownDefinitions() {
        DynamicModuleRegistry registry = new DynamicModuleRegistry();
        registry.register(contractModule());

        assertThatThrownBy(() -> registry.register(contractModule()))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("duplicate module alias");
        assertThatThrownBy(() -> registry.requireModule("sales.order"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown module alias");
        assertThatThrownBy(() -> registry.requireEntity("sales.contract", "missing"))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("unknown entity");
    }

    private ModuleDefinition contractModule() {
        return new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(new EntityDefinition(
                        "contract",
                        "app_contract",
                        "Contract",
                        List.of(
                                FieldDefinition.string("code", "Code").length(64).required(),
                                FieldDefinition.decimal("amount", "Amount").precision(18, 2)
                        )
                ))
        );
    }
}
