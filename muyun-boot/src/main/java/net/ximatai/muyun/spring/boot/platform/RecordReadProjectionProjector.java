package net.ximatai.muyun.spring.boot.platform;

import net.ximatai.muyun.spring.common.schema.StandardEntitySchema;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        Map<String, PropertyDescriptor> properties = properties(record.getClass());
        LinkedHashMap<String, Object> output = new LinkedHashMap<>();
        for (String fieldName : responseFields(projection)) {
            PropertyDescriptor property = properties.get(fieldName);
            if (property != null && property.getReadMethod() != null) {
                output.put(fieldName, read(record, property));
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
                        java.util.stream.Stream.of(StandardEntitySchema.ID_FIELD),
                        projection.outputFields().stream().map(ViewFieldRef::fieldName))
                .distinct()
                .toList();
    }

    private static Map<String, PropertyDescriptor> properties(Class<?> recordClass) {
        try {
            return java.util.Arrays.stream(Introspector.getBeanInfo(recordClass).getPropertyDescriptors())
                    .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity(), (left, right) -> left));
        } catch (IntrospectionException ex) {
            throw new IllegalArgumentException("record properties cannot be read: " + recordClass.getName(), ex);
        }
    }

    private static Object read(Object record, PropertyDescriptor property) {
        try {
            return property.getReadMethod().invoke(record);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalArgumentException("record property cannot be read: " + property.getName(), ex);
        }
    }
}
