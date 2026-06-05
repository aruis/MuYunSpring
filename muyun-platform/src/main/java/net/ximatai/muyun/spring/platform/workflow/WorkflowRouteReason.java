package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowRouteReason implements CodeTitleEnum {
    MANUAL_SELECTED("manual_selected", "手动选中"),
    MANUAL_UNSELECTED("manual_unselected", "手动未选"),
    CONDITION_MATCHED("condition_matched", "条件命中"),
    CONDITION_UNMATCHED("condition_unmatched", "条件未命中"),
    DEFAULT_SELECTED("default_selected", "默认选中"),
    CONVERGE_REACHED("converge_reached", "到达汇聚"),
    NORMAL_CONVERGED("normal_converged", "普通汇聚");

    private final String code;
    private final String title;

    WorkflowRouteReason(String code, String title) {
        this.code = code;
        this.title = title;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
