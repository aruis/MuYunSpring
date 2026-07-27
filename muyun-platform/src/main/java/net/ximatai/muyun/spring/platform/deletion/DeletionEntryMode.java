package net.ximatai.muyun.spring.platform.deletion;

import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

/**
 * The data-retention mode selected by the owning resource module.
 */
public enum DeletionEntryMode implements CodeTitleEnum {
    SOFT("soft", "Soft delete"),
    HARD("hard", "Hard delete");

    private final String code;
    private final String title;

    DeletionEntryMode(String code, String title) {
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
