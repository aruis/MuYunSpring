package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.formula.FormulaEngine;
import net.ximatai.muyun.spring.common.formula.FormulaExecutionResult;
import net.ximatai.muyun.spring.common.formula.FormulaFieldDefinition;
import net.ximatai.muyun.spring.common.formula.FormulaRule;
import net.ximatai.muyun.spring.common.formula.FormulaRulePhase;
import net.ximatai.muyun.spring.common.formula.FormulaRuntimeData;
import net.ximatai.muyun.spring.common.formula.FormulaRuntimeReport;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicFormulaFieldDefinitions;
import net.ximatai.muyun.spring.dynamic.metadata.EntityDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityFormulaRuleDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.EntityRelationDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.ModuleDefinition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DynamicFormulaRuntime {
    private final FormulaEngine engine = new FormulaEngine();
    private final EntityDefinition entity;
    private final ModuleDefinition module;

    DynamicFormulaRuntime(EntityDefinition entity, ModuleDefinition module) {
        this.entity = entity;
        this.module = module;
    }

    void beforeInsert(DynamicRecord record) {
        execute(record, null, List.of(FormulaRulePhase.DEFAULT_VALUE, FormulaRulePhase.BEFORE_SAVE), true);
    }

    void beforeUpdate(DynamicRecord record, DynamicRecord existing) {
        execute(record, existing, List.of(FormulaRulePhase.BEFORE_SAVE), false);
    }

    boolean hasBeforeUpdateRules() {
        return hasRules(List.of(FormulaRulePhase.BEFORE_SAVE), false);
    }

    private boolean hasRules(List<FormulaRulePhase> phases, boolean includeChildDependentRules) {
        return entity.orderedFormulaRules().stream()
                .anyMatch(rule -> rule.enabled()
                        && phases.contains(rule.phase())
                        && (includeChildDependentRules || !dependsOnChildRows(rule)));
    }

    private void execute(DynamicRecord record,
                         DynamicRecord existing,
                         List<FormulaRulePhase> phases,
                         boolean includeChildDependentRules) {
        List<FormulaRule> rules = runtimeRules(phases, includeChildDependentRules);
        if (rules.isEmpty()) {
            return;
        }
        Map<String, Object> main = mainValues(record, existing);
        Map<String, List<Map<String, Object>>> tables = childValues(record);
        FormulaExecutionResult result = engine.execute(rules, FormulaRuntimeData.typed(main, tables, fieldDefinitions()));
        if (result.report().hasErrors()) {
            throw new IllegalArgumentException(formulaErrorMessage(result.report()));
        }
        applyChangedFields(record, main, tables, result.changedFields());
    }

    private List<FormulaRule> runtimeRules(List<FormulaRulePhase> phases, boolean includeChildDependentRules) {
        return entity.orderedFormulaRules().stream()
                .filter(EntityFormulaRuleDefinition::enabled)
                .filter(rule -> phases.contains(rule.phase()))
                .filter(rule -> includeChildDependentRules || !dependsOnChildRows(rule))
                .map(EntityFormulaRuleDefinition::toRuntimeRule)
                .toList();
    }

    private boolean dependsOnChildRows(EntityFormulaRuleDefinition rule) {
        if (module == null) {
            return false;
        }
        for (EntityRelationDefinition relation : module.relations()) {
            if (!entity.code().equals(relation.parentEntity())) {
                continue;
            }
            String prefix = relation.code() + ".";
            if (rule.targetField() != null && rule.targetField().startsWith(prefix)) {
                return true;
            }
            if (rule.expression() != null && engine.referencedFields(rule.expression()).stream()
                    .anyMatch(field -> field.startsWith(prefix))) {
                return true;
            }
        }
        return false;
    }

    private List<FormulaFieldDefinition> fieldDefinitions() {
        List<FormulaFieldDefinition> definitions = new ArrayList<>(DynamicFormulaFieldDefinitions.mainFields(entity));
        if (module != null) {
            for (EntityRelationDefinition relation : module.relations()) {
                if (!entity.code().equals(relation.parentEntity())) {
                    continue;
                }
                module.entities().stream()
                        .filter(candidate -> relation.childEntity().equals(candidate.code()))
                        .findFirst()
                        .ifPresent(child -> definitions.addAll(
                                DynamicFormulaFieldDefinitions.childFields(relation.code(), child)
                        ));
            }
        }
        return List.copyOf(definitions);
    }

    private Map<String, Object> mainValues(DynamicRecord record, DynamicRecord existing) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (existing != null) {
            values.putAll(existing.getValues());
        }
        values.putAll(record.getValues());
        return values;
    }

    private Map<String, List<Map<String, Object>>> childValues(DynamicRecord record) {
        Map<String, List<Map<String, Object>>> values = new LinkedHashMap<>();
        record.getChildren().forEach((relationCode, children) -> {
            if (children == null) {
                return;
            }
            values.put(relationCode, children.stream()
                    .<Map<String, Object>>map(child -> new LinkedHashMap<>(child.getValues()))
                    .toList());
        });
        return values;
    }

    private void applyChangedFields(DynamicRecord record,
                                    Map<String, Object> main,
                                    Map<String, List<Map<String, Object>>> tables,
                                    List<String> changedFields) {
        for (String dataIndex : changedFields) {
            int dot = dataIndex.indexOf('.');
            if (dot < 0) {
                record.setValue(dataIndex, main.get(dataIndex));
                continue;
            }
            String relationCode = dataIndex.substring(0, dot);
            String fieldName = dataIndex.substring(dot + 1);
            List<DynamicRecord> children = record.getChildren(relationCode);
            List<Map<String, Object>> rows = tables.get(relationCode);
            if (children == null || rows == null) {
                continue;
            }
            for (int i = 0; i < children.size() && i < rows.size(); i++) {
                children.get(i).setValue(fieldName, rows.get(i).get(fieldName));
            }
        }
    }

    private String formulaErrorMessage(FormulaRuntimeReport report) {
        FormulaRuntimeReport.Issue issue = report.errors().getFirst();
        String ruleId = issue.ruleId() == null ? "formula" : issue.ruleId();
        String message = issue.message() == null ? issue.code() : issue.message();
        return "dynamic formula rule failed: " + ruleId + ", " + message;
    }
}
