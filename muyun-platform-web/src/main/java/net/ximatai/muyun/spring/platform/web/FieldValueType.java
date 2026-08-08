package net.ximatai.muyun.spring.platform.web;

import net.ximatai.muyun.spring.dynamic.metadata.FieldType;

/** Source-neutral value type published with resolved module view fields. */
public enum FieldValueType {
    STRING,
    TEXT,
    INTEGER,
    LONG,
    BOOLEAN,
    TIMESTAMP,
    ZONED_TIMESTAMP,
    DATE,
    DECIMAL,
    JSON;

    public static FieldValueType from(FieldType type) {
        return type == null ? null : valueOf(type.name());
    }
}
