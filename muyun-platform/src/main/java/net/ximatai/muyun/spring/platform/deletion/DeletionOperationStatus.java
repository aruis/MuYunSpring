package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

/**
 * Aggregate execution state of one deletion lifecycle command.
 */
public enum DeletionOperationStatus implements CodeTitleEnum {
    IN_PROGRESS("inProgress", "In progress"),
    SUCCEEDED("succeeded", "Succeeded"),
    PARTIALLY_SUCCEEDED("partiallySucceeded", "Partially succeeded"),
    FAILED("failed", "Failed");

    private final String code;
    private final String title;

    DeletionOperationStatus(String code, String title) {
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
