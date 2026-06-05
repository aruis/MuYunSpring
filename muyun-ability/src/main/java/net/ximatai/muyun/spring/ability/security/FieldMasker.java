package net.ximatai.muyun.spring.ability.security;

import net.ximatai.muyun.spring.common.security.FieldMaskingPolicy;
import net.ximatai.muyun.spring.common.security.FieldOutputContext;

public interface FieldMasker {
    FieldMasker DEFAULT = new DefaultFieldMasker();

    Object mask(String fieldName, Object value, FieldMaskingPolicy policy, FieldOutputContext context);
}
