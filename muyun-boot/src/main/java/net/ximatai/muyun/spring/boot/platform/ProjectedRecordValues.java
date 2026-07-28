package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.model.contract.EntityContract;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

/** Typed access to platform identity fields across entity and map-backed read projections. */
public final class ProjectedRecordValues {
    private ProjectedRecordValues() {
    }

    public static String id(Object record) {
        if (record instanceof EntityContract entity) {
            return entity.getId();
        }
        Object value = value(record, "id");
        if (value == null) {
            throw new IllegalArgumentException("projected record must expose id");
        }
        return value.toString();
    }

    public static Instant deletedAt(Object record) {
        if (record instanceof EntityContract entity) {
            return entity.getDeletedAt();
        }
        Object value = value(record, "deletedAt");
        if (value == null) return null;
        if (value instanceof Instant instant) return instant;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toInstant();
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        }
        if (value instanceof CharSequence text && !text.toString().isBlank()) {
            return Instant.parse(text.toString());
        }
        throw new IllegalArgumentException("projected record deletedAt has unsupported type: "
                + value.getClass().getName());
    }

    private static Object value(Object record, String field) {
        if (record instanceof Map<?, ?> values) {
            return values.get(field);
        }
        throw new IllegalArgumentException("projected record must be an entity or map-backed projection");
    }
}
