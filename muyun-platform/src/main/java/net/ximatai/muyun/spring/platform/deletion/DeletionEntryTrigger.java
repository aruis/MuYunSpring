package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

/**
 * Whether an affected resource is the root command target or a cascading target.
 */
public enum DeletionEntryTrigger implements CodeTitleEnum {
    DIRECT("direct", "Direct"),
    CASCADE("cascade", "Cascade");

    private final String code;
    private final String title;

    DeletionEntryTrigger(String code, String title) {
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
