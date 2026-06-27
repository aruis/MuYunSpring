package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.common.model.capability.TitledCapable;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;

public interface RecordLabelWeb<T extends EntityContract> {
    default String recordLabel(T record) {
        if (record == null) {
            return null;
        }
        if (record instanceof TitledCapable titled && hasText(titled.getTitle())) {
            return titled.getTitle().trim();
        }
        return record.getId();
    }

    default String successMessage(T record, String actionText) {
        String label = recordLabel(record);
        if (!hasText(label)) {
            return actionText;
        }
        return "「" + label.trim() + "」" + actionText;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
