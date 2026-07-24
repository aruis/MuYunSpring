package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

/**
 * The lifecycle command represented by a deletion-log operation.
 */
public enum DeletionOperationType implements CodeTitleEnum {
    DELETE("delete", "Delete"),
    RESTORE("restore", "Restore"),
    PURGE("purge", "Purge");

    private final String code;
    private final String title;

    DeletionOperationType(String code, String title) {
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
