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
    void shouldRejectCustomActionThatConflictsWithPlatformStandardPath() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(customAction("create"))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("custom action conflicts with reserved web action path: contract.create");
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

    @Test
    void shouldRejectStandardActionConfiguredWithCustomExecutor() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition("contract", "query", EntityActionKind.COLLECTION,
                        "Query", true, EntityActionLevel.LIST, EntityActionStyle.NORMAL,
                        EntityActionCategory.STANDARD, EntityActionAccessMode.AUTH_REQUIRED,
                        true, false, null, null, null, EntityActionExecutorType.SERVICE, "queryExecutor"))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("standard action executor must be STANDARD: contract.query");
    }

    @Test
    void shouldRejectStandardCategoryWhenActionIsNotPlatformStandardAction() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition("contract", "submit", EntityActionKind.CUSTOM,
                        "Submit", true, EntityActionLevel.RECORD, EntityActionStyle.NORMAL,
                        EntityActionCategory.STANDARD, EntityActionAccessMode.AUTH_REQUIRED,
                        true, false, null, null, null, EntityActionExecutorType.STANDARD, null))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("standard action is not supported by entity: contract.submit");
    }

    @Test
    void shouldRejectDialogActionWithoutExecutorKey() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition("contract", "submitDialog", EntityActionKind.CUSTOM,
                        "Submit Dialog", true, EntityActionLevel.RECORD, EntityActionStyle.PRIMARY,
                        EntityActionCategory.DIALOG, EntityActionAccessMode.AUTH_REQUIRED,
                        true, false, null, null, null, EntityActionExecutorType.DIALOG, null))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("dialog action requires executor key: submitDialog");
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
