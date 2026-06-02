package net.ximatai.muyun.spring.dynamic.metadata;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleDefinitionValidatorTest {
    private final ModuleDefinitionValidator validator = new ModuleDefinitionValidator();

    @Test
    void shouldRejectCustomActionThatConflictsWithReservedWebPath() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(customAction("query"))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("custom action conflicts with reserved web action path: contract.query");
    }

    @Test
    void shouldRejectConfiguredStandardActionWithCustomCategoryOnReservedPath() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition("contract", "delete", EntityActionKind.RECORD,
                        "Delete", true, EntityActionLevel.RECORD, EntityActionStyle.DANGER,
                        EntityActionCategory.CUSTOM, EntityActionAccessMode.AUTH_REQUIRED,
                        true, false, null, null, null, EntityActionExecutorType.SERVICE, "deleteExecutor"))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("custom action conflicts with reserved web action path: contract.delete");
    }

    private EntityActionDefinition customAction(String actionCode) {
        return new EntityActionDefinition("contract", actionCode, EntityActionKind.CUSTOM,
                "Custom " + actionCode, true, EntityActionLevel.LIST, EntityActionStyle.NORMAL,
                EntityActionCategory.CUSTOM, EntityActionAccessMode.AUTH_REQUIRED,
                true, false, null, null, null, EntityActionExecutorType.SERVICE, actionCode + "Executor");
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required()
        ));
    }
}
