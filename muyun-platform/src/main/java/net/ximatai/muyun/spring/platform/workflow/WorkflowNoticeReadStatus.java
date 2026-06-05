package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public enum WorkflowNoticeReadStatus implements CodeTitleEnum {
    ALL("all", "全部"),
    UNREAD("unread", "未读"),
    READ("read", "已读");

    private final String code;
    private final String title;

    WorkflowNoticeReadStatus(String code, String title) {
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
