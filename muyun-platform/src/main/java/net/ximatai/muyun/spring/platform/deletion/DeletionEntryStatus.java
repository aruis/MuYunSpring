package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

/**
 * Execution outcome of one affected resource in an operation.
 */
public enum DeletionEntryStatus implements CodeTitleEnum {
    IN_PROGRESS("inProgress", "In progress"),
    SUCCEEDED("succeeded", "Succeeded"),
    SKIPPED("skipped", "Skipped"),
    FAILED("failed", "Failed");

    private final String code;
    private final String title;

    DeletionEntryStatus(String code, String title) {
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
