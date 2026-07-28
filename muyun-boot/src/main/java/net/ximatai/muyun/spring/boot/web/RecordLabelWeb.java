package net.ximatai.muyun.spring.boot.web;

import net.ximatai.muyun.spring.ability.security.FieldProtectionAbility;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.model.title.RecordLabelResolver;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;

public interface RecordLabelWeb<T extends EntityContract> {
    Object service();

    default String recordLabel(T record) {
        String label = RecordLabelResolver.readAsString(record);
        if (label == null || !(service() instanceof FieldProtectionAbility<?> protectionAbility)) {
            return label;
        }
        String fieldName = RecordLabelResolver.resolveFieldName(record.getClass()).orElse(null);
        Object protectedLabel = protectionAbility.maskProtectedValue(fieldName, label, FieldOutputContext.VIEW);
        return protectedLabel == null ? null : String.valueOf(protectedLabel);
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
