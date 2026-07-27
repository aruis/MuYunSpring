package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecordReadProjectionProjector {
    private RecordReadProjectionProjector() {
    }

    public static Map<String, Object> project(Object record, RecordReadProjection projection) {
        if (record == null) {
            return Map.of();
        }
        if (projection == null) {
            throw new IllegalArgumentException("record read projection must not be null");
        }
        BeanWrapper wrapper = PropertyAccessorFactory.forBeanPropertyAccess(record);
        LinkedHashMap<String, Object> output = new LinkedHashMap<>();
        for (String fieldName : responseFields(projection)) {
            if (wrapper.isReadableProperty(fieldName)) {
                output.put(fieldName, wrapper.getPropertyValue(fieldName));
            }
        }
        return Collections.unmodifiableMap(output);
    }

    public static List<Map<String, Object>> project(List<?> records, RecordReadProjection projection) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .map(record -> project(record, projection))
                .toList();
    }

    private static List<String> responseFields(RecordReadProjection projection) {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(
                                StandardEntitySchema.ID_FIELD,
                                StandardEntitySchema.VERSION_FIELD,
                                StandardEntitySchema.DELETED_AT_FIELD
                        ),
                        projection.outputFields().stream().map(ViewFieldRef::fieldName))
                .distinct()
                .toList();
    }
}
