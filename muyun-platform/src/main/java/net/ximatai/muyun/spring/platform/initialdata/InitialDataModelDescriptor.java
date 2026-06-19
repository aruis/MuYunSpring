package net.ximatai.muyun.spring.platform.initialdata;

import net.ximatai.muyun.spring.ability.initialdata.InitialDataPolicy;
import net.ximatai.muyun.spring.common.model.contract.EntityContract;
import net.ximatai.muyun.spring.common.initialdata.InitialDataFields;
import net.ximatai.muyun.spring.common.initialdata.InitialDataRole;
import net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataRecord;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class InitialDataModelDescriptor<T extends EntityContract> {
    private static final Map<Class<?>, InitialDataModelDescriptor<?>> CACHE = new ConcurrentHashMap<>();

    private final List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<T>> identityFields;
    private final List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<T>> managedFields;
    private final List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<T>> operatorFields;

    private InitialDataModelDescriptor(
            List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<T>> identityFields,
            List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<T>> managedFields,
            List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<T>> operatorFields) {
        this.identityFields = List.copyOf(identityFields);
        this.managedFields = List.copyOf(managedFields);
        this.operatorFields = List.copyOf(operatorFields);
    }

    static <T extends EntityContract> InitialDataModelDescriptor<T> of(Class<T> modelClass) {
        @SuppressWarnings("unchecked")
        InitialDataModelDescriptor<T> descriptor = (InitialDataModelDescriptor<T>) CACHE.computeIfAbsent(modelClass,
                InitialDataModelDescriptor::compile);
        return descriptor;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    InitialDataRecord<T> record(String key, InitialDataPolicy policy, T desired) {
        InitialDataRecord<T> record = InitialDataRecord.of(key, policy, desired)
                .identity(identityFields.toArray(net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField[]::new))
                .managed(managedFields.toArray(net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField[]::new))
                .operator(operatorFields.toArray(net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField[]::new));
        return record;
    }

    private static InitialDataModelDescriptor<?> compile(Class<?> modelClass) {
        List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<EntityContract>> identityFields =
                new ArrayList<>();
        List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<EntityContract>> managedFields =
                new ArrayList<>();
        List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<EntityContract>> operatorFields =
                new ArrayList<>();

        Map<String, Field> fields = fields(modelClass);
        InitialDataFields classAnnotation = modelClass.getAnnotation(InitialDataFields.class);
        if (classAnnotation == null || classAnnotation.includeId()) {
            identityFields.add(net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField.of(
                    "id", EntityContract::getId, EntityContract::setId));
        }
        if (classAnnotation != null) {
            addDeclaredFields(identityFields, fields, classAnnotation.identity(), modelClass);
            addDeclaredFields(managedFields, fields, classAnnotation.managed(), modelClass);
            addDeclaredFields(operatorFields, fields, classAnnotation.operator(), modelClass);
        }
        for (Field field : fields.values()) {
            InitialDataRole annotation = field.getAnnotation(InitialDataRole.class);
            if (annotation == null) {
                continue;
            }
            net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<EntityContract> descriptor =
                    fieldDescriptor(field);
            switch (annotation.value()) {
                case IDENTITY -> identityFields.add(descriptor);
                case MANAGED -> managedFields.add(descriptor);
                case OPERATOR -> operatorFields.add(descriptor);
            }
        }
        return new InitialDataModelDescriptor<>(identityFields, managedFields, operatorFields);
    }

    private static Map<String, Field> fields(Class<?> modelClass) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = modelClass;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        fields.sort(Comparator.comparing(Field::getName));
        Map<String, Field> result = new LinkedHashMap<>();
        for (Field field : fields) {
            result.putIfAbsent(field.getName(), field);
        }
        return result;
    }

    private static void addDeclaredFields(
            List<net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<EntityContract>> target,
            Map<String, Field> fields,
            String[] fieldNames,
            Class<?> modelClass) {
        if (fieldNames == null) {
            return;
        }
        for (String fieldName : fieldNames) {
            Field field = fields.get(fieldName);
            if (field == null) {
                throw new IllegalStateException("Initial data field not found: "
                        + modelClass.getName() + "." + fieldName);
            }
            target.add(fieldDescriptor(field));
        }
    }

    private static net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField<EntityContract> fieldDescriptor(
            Field field) {
        field.setAccessible(true);
        return net.ximatai.muyun.spring.platform.initialdata.spi.InitialDataField.of(
                field.getName(),
                source -> read(field, source),
                (target, value) -> write(field, target, value));
    }

    private static Object read(Field field, Object source) {
        try {
            return field.get(source);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot read initial data field: " + field.getName(), ex);
        }
    }

    private static void write(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException("Cannot write initial data field: " + field.getName(), ex);
        }
    }
}
