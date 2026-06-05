package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowNodeType implements CodeTitleEnum {
    START("start", "开始"),
    APPROVAL("approval", "审批"),
    TASK("task", "任务"),
    BRANCH("branch", "分支"),
    CONVERGE("converge", "汇聚"),
    MILESTONE("milestone", "里程碑"),
    END("end", "结束");

    private final String code;
    private final String title;

    WorkflowNodeType(String code, String title) {
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
