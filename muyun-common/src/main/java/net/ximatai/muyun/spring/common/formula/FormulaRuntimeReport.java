package net.ximatai.muyun.spring.common.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormulaRuntimeReport {
    private final List<Issue> warnings = new ArrayList<>();
    private final List<Issue> errors = new ArrayList<>();

    public List<Issue> warnings() {
        return Collections.unmodifiableList(warnings);
    }

    public List<Issue> errors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void warn(String ruleId, String expression, String message) {
        warnings.add(new Issue("FORMULA_WARNING", FormulaIssueLevel.WARNING, ruleId, null, null, null, expression, message));
    }

    public void error(String ruleId, String expression, String message) {
        errors.add(new Issue("FORMULA_ERROR", FormulaIssueLevel.ERROR, ruleId, null, null, null, expression, message));
    }

    public void error(
            FormulaRule rule,
            String code,
            String fieldPath,
            Integer position,
            String expression,
            String message
    ) {
        errors.add(issue(rule, code, FormulaIssueLevel.ERROR, fieldPath, position, expression, message));
    }

    public void add(
            FormulaRule rule,
            String code,
            String fieldPath,
            Integer position,
            String expression,
            String message
    ) {
        Issue issue = issue(rule, code, rule.severity(), fieldPath, position, expression, message);
        if (issue.level() == FormulaIssueLevel.WARNING) {
            warnings.add(issue);
        } else {
            errors.add(issue);
        }
    }

    private Issue issue(
            FormulaRule rule,
            String code,
            FormulaIssueLevel level,
            String fieldPath,
            Integer position,
            String expression,
            String message
    ) {
        return new Issue(
                code == null || code.isBlank() ? "FORMULA_ERROR" : code,
                level == null ? FormulaIssueLevel.ERROR : level,
                rule == null ? null : rule.id(),
                fieldPath,
                position,
                rule == null ? null : rule.phase(),
                expression,
                message
        );
    }

    public record Issue(
            String code,
            FormulaIssueLevel level,
            String ruleId,
            String fieldPath,
            Integer position,
            FormulaRulePhase phase,
            String expression,
            String message
    ) {
    }
}
