package net.ximatai.muyun.spring.common.schema;

import net.ximatai.muyun.database.core.orm.DatabaseValueConverter;
import net.ximatai.muyun.spring.common.model.contract.CodeTitleEnum;

public final class PlatformDatabaseValueConverter implements DatabaseValueConverter {

    @Override
    public Object toDatabaseValue(Object value) {
        if (value instanceof CodeTitleEnum codeTitleEnum) {
            return codeTitleEnum.getCode();
        }
        return DatabaseValueConverter.DEFAULT.toDatabaseValue(value);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object fromDatabaseValue(Object value, Class<?> targetType) {
        if (value != null && targetType.isEnum() && CodeTitleEnum.class.isAssignableFrom(targetType)) {
            String text = String.valueOf(value);
            for (Object constant : targetType.getEnumConstants()) {
                CodeTitleEnum codeTitleEnum = (CodeTitleEnum) constant;
                if (text.equals(codeTitleEnum.getCode())) {
                    return constant;
                }
            }
        }
        return DatabaseValueConverter.DEFAULT.fromDatabaseValue(value, targetType);
    }
}
