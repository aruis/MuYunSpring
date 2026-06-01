package net.ximatai.muyun.spring.common.formula;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FormulaExecutionResult {
    private final FormulaRuntimeReport report = new FormulaRuntimeReport();
    private final List<FormulaRuleDecision> decisions = new ArrayList<>();
    private final List<String> changedFields = new ArrayList<>();

    public FormulaRuntimeReport report() {
        return report;
    }

    public List<FormulaRuleDecision> decisions() {
        return Collections.unmodifiableList(decisions);
    }

    public List<String> changedFields() {
        return Collections.unmodifiableList(changedFields);
    }

    void decide(FormulaRule rule, boolean matched) {
        decisions.add(new FormulaRuleDecision(rule.id(), rule.kind(), rule.phase(), rule.targetField(), matched));
    }

    void changed(FormulaFieldPath fieldPath) {
        String dataIndex = fieldPath.dataIndex();
        if (!changedFields.contains(dataIndex)) {
            changedFields.add(dataIndex);
        }
    }
}
