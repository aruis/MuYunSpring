package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.formula.FormulaEngine;
import net.ximatai.muyun.spring.common.formula.FormulaExecutionResult;
import net.ximatai.muyun.spring.common.formula.FormulaIssueLevel;
import net.ximatai.muyun.spring.common.formula.FormulaRule;
import net.ximatai.muyun.spring.common.formula.FormulaRuleKind;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import net.ximatai.muyun.spring.common.formula.FormulaRuntimeData;
import net.ximatai.muyun.spring.common.formula.FormulaRuntimeReport;
import net.ximatai.muyun.spring.dynamic.metadata.EntityActionDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;

import java.util.List;
import java.util.Map;

final class DynamicActionAvailabilityRuntime {
    private final FormulaEngine engine = new FormulaEngine();
    private final EntityDefinition entity;
    private final ModuleDefinition module;

    DynamicActionAvailabilityRuntime(EntityDefinition entity, ModuleDefinition module) {
        this.entity = entity;
        this.module = module;
    }

    DynamicActionAvailability evaluate(EntityActionDefinition action, DynamicRecord record, DynamicRecord existing) {
        if (!action.enabled()) {
            return DynamicActionAvailability.unavailable(action.actionCode(), disabledMessage(action));
        }
        if (!action.hasAvailabilityCondition()) {
            return DynamicActionAvailability.available(action.actionCode());
        }
        FormulaRule rule = new FormulaRule(
                action.actionCode(),
                action.availableExpression(),
                FormulaRuleKind.CONDITION,
                FormulaRulePhase.ACTION_AVAILABLE,
                null,
                FormulaIssueLevel.ERROR,
                action.unavailableMessage(),
                true,
                true
        );
        Map<String, Object> main = DynamicFormulaDataSupport.mainValues(record, existing);
        Map<String, List<Map<String, Object>>> tables = DynamicFormulaDataSupport.childValues(record);
        FormulaExecutionResult result = engine.execute(List.of(rule), FormulaRuntimeData.typed(
                main, tables, DynamicFormulaDataSupport.fieldDefinitions(entity, module)));
        if (!result.report().hasErrors()) {
            return new DynamicActionAvailability(action.actionCode(), true, null, result.report());
        }
        return new DynamicActionAvailability(action.actionCode(), false,
                firstMessage(result.report(), action.unavailableMessage()), result.report());
    }

    private String firstMessage(FormulaRuntimeReport report, String fallback) {
        if (!report.errors().isEmpty() && report.errors().getFirst().message() != null) {
            return report.errors().getFirst().message();
        }
        return fallback;
    }

    private String disabledMessage(EntityActionDefinition action) {
        return action.unavailableMessage() == null ? "action is disabled" : action.unavailableMessage();
    }
}
