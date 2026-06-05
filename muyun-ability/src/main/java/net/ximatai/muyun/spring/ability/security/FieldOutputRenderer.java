package net.ximatai.muyun.spring.ability.security;

import net.ximatai.muyun.spring.common.security.FieldOutputContext;
import net.ximatai.muyun.spring.common.security.FieldProtectionDefinition;

public final class FieldOutputRenderer {
    private FieldOutputRenderer() {
    }

    public static Object renderValue(String fieldName,
                                     Object value,
                                     FieldProtectionDefinition protection,
                                     FieldOutputContext context,
                                     FieldMasker masker) {
        if (value == null || protection == null || !protection.hasOutputProtection()) {
            return value;
        }
        FieldMasker effectiveMasker = masker == null ? FieldMasker.DEFAULT : masker;
        return effectiveMasker.mask(fieldName, value, protection.maskingPolicy(), context);
    }

    public static <T> T renderRecord(T entity,
                                     FieldProtectionPlan<T> plan,
                                     FieldOutputContext context,
                                     FieldMasker masker) {
        if (entity == null || plan == null || plan.isEmpty()) {
            return entity;
        }
        for (ProtectedFieldAccessor<T> field : plan.fields()) {
            if (field.protection().hasOutputProtection() && field.hasValue(entity)) {
                field.set(entity, renderValue(field.fieldName(), field.get(entity),
                        field.protection(), context, masker));
            }
            if (field.protection().signatureMode().enabled()) {
                field.setSignature(entity, null);
            }
        }
        return entity;
    }
}
