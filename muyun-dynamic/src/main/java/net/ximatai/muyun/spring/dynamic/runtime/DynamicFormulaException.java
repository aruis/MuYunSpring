package net.ximatai.muyun.spring.dynamic.runtime;

import net.ximatai.muyun.spring.common.exception.PlatformException;
import net.ximatai.muyun.spring.common.formula.FormulaRuntimeReport;

import java.util.List;

public class DynamicFormulaException extends PlatformException {
    private final String moduleAlias;
    private final String entityCode;
    private final List<FormulaRuntimeReport.Issue> errors;
    private final List<FormulaRuntimeReport.Issue> warnings;

    public DynamicFormulaException(String moduleAlias,
                                   String entityCode,
                                   FormulaRuntimeReport report) {
        super(message(moduleAlias, entityCode, report));
        this.moduleAlias = moduleAlias;
        this.entityCode = entityCode;
        this.errors = report == null ? List.of() : List.copyOf(report.errors());
        this.warnings = report == null ? List.of() : List.copyOf(report.warnings());
    }

    public String moduleAlias() {
        return moduleAlias;
    }

    public String entityCode() {
        return entityCode;
    }

    public List<FormulaRuntimeReport.Issue> errors() {
        return errors;
    }

    public List<FormulaRuntimeReport.Issue> warnings() {
        return warnings;
    }

    public FormulaRuntimeReport.Issue firstError() {
        return errors.isEmpty() ? null : errors.getFirst();
    }

    private static String message(String moduleAlias, String entityCode, FormulaRuntimeReport report) {
        FormulaRuntimeReport.Issue issue = report == null || report.errors().isEmpty()
                ? null
                : report.errors().getFirst();
        String ruleId = issue == null || issue.ruleId() == null ? "formula" : issue.ruleId();
        String errorMessage = issue == null || issue.message() == null ? "formula rule failed" : issue.message();
        String target = moduleAlias == null || entityCode == null ? "" : moduleAlias + "." + entityCode + ": ";
        return "dynamic formula rule failed: " + target + ruleId + ", " + errorMessage;
    }
}
