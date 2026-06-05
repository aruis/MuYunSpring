package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowEventType implements CodeTitleEnum {
    INSTANCE_STARTED("instance_started", "流程启动"),
    NODE_ACTIVATED("node_activated", "节点激活"),
    TASK_CREATED("task_created", "任务创建"),
    TASK_COMPLETED("task_completed", "任务完成"),
    TASK_REJECTED("task_rejected", "任务驳回"),
    TASK_RESUBMITTED("task_resubmitted", "任务重提"),
    TASK_TRANSFERRED("task_transferred", "任务转办"),
    TASK_SKIPPED("task_skipped", "任务跳过"),
    TASK_INVALIDATED("task_invalidated", "任务失效"),
    TASK_CANCELED("task_canceled", "任务取消"),
    ROUTE_SELECTED("route_selected", "路径选中"),
    ROUTE_DROPPED("route_dropped", "路径裁剪"),
    NODE_ROLLED_BACK("node_rolled_back", "节点回退"),
    APPROVAL_COMPLETED("approval_completed", "审批完成"),
    APPROVAL_UNAPPROVED("approval_unapproved", "弃审"),
    INSTANCE_COMPLETED("instance_completed", "流程完成"),
    INSTANCE_REVOKED("instance_revoked", "流程撤回"),
    INSTANCE_RESET("instance_reset", "流程重置"),
    INSTANCE_REJECTED("instance_rejected", "流程驳回"),
    INSTANCE_TERMINATED("instance_terminated", "流程终止"),
    OVERTIME_WARNED("overtime_warned", "超期预警");

    private final String code;
    private final String title;

    WorkflowEventType(String code, String title) {
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
