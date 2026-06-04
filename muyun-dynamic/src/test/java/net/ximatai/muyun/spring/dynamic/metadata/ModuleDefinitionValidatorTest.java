package net.ximatai.muyun.spring.dynamic.metadata;


import net.ximatai.muyun.spring.common.platform.EntityCapability;
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
                List.of(new EntityActionDefinition("contract", "delete", "Delete", true, EntityActionLevel.RECORD,
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
                List.of(new EntityActionDefinition("contract", "query", "Query", true, EntityActionLevel.LIST,
                        EntityActionCategory.STANDARD, EntityActionAccessMode.AUTH_REQUIRED,
                        true, false, null, null, null, EntityActionExecutorType.SERVICE, "queryExecutor"))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("standard action executor must be STANDARD: contract.query");
    }

    @Test
    void shouldRejectStandardActionConfiguredWithWrongLevel() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition("contract", "delete", "Delete", true, EntityActionLevel.LIST,
                        EntityActionCategory.STANDARD, EntityActionAccessMode.AUTH_REQUIRED,
                        true, false, null, null, null, EntityActionExecutorType.STANDARD, null))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("standard action level must match platform action: contract.delete");
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
                List.of(new EntityActionDefinition("contract", "submit", "Submit", true, EntityActionLevel.RECORD,
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
                List.of(new EntityActionDefinition("contract", "submitDialog", "Submit Dialog", true, EntityActionLevel.RECORD,
                        EntityActionCategory.DIALOG, EntityActionAccessMode.AUTH_REQUIRED,
                        true, false, null, null, null, EntityActionExecutorType.DIALOG, null))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("dialog action requires executor key: submitDialog");
    }

    @Test
    void shouldRequireDataScopeCapabilityForDataAuthAction() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition("contract", "submit", "Submit", true, EntityActionLevel.RECORD,
                        EntityActionCategory.CUSTOM, EntityActionAccessMode.AUTH_REQUIRED,
                        true, true, null, null, null, EntityActionExecutorType.SERVICE, "submitExecutor"))
        );

        assertThatThrownBy(() -> validator.validate(module))
                .isInstanceOf(ModuleDefinitionException.class)
                .hasMessageContaining("data auth action requires DATA_SCOPE capability: contract.submit");
    }

    @Test
    void shouldAllowDataAuthActionWhenEntitySupportsDataScope() {
        ModuleDefinition module = new ModuleDefinition(
                "sales.contract",
                "Contract",
                List.of(contractEntity().withCapabilities(EntityCapability.DATA_SCOPE)),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(new EntityActionDefinition("contract", "submit", "Submit", true, EntityActionLevel.RECORD,
                        EntityActionCategory.CUSTOM, EntityActionAccessMode.AUTH_REQUIRED,
                        true, true, null, null, null, EntityActionExecutorType.SERVICE, "submitExecutor"))
        );

        validator.validate(module);
    }

    private EntityActionDefinition customAction(String actionCode) {
        return new EntityActionDefinition("contract", actionCode, "Custom " + actionCode, true, EntityActionLevel.LIST,
                EntityActionCategory.CUSTOM, EntityActionAccessMode.AUTH_REQUIRED,
                true, false, null, null, null, EntityActionExecutorType.SERVICE, actionCode + "Executor");
    }

    private EntityDefinition contractEntity() {
        return new EntityDefinition("contract", "sales_contract", "Contract", List.of(
                FieldDefinition.string("code", "Code").length(64).required()
        ));
    }
}
