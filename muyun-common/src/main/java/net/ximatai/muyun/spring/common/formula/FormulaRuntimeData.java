package net.ximatai.muyun.spring.common.formula;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FormulaRuntimeData implements FormulaEvaluationContext {
    private final RowValue main;
    private final Map<String, List<RowValue>> tables;
    private final boolean strictFields;
    private final boolean typedFields;
    private final Set<FormulaFieldPath> knownFields;
    private final Set<String> knownTables;
    private final Map<FormulaFieldPath, FormulaFieldDefinition> fieldDefinitions;

    public FormulaRuntimeData(Map<String, Object> main, Map<String, List<Map<String, Object>>> tables) {
        this(main, tables, false, false, Set.of(), Map.of());
    }

    private FormulaRuntimeData(
            Map<String, Object> main,
            Map<String, List<Map<String, Object>>> tables,
            boolean strictFields,
            boolean typedFields,
            Set<FormulaFieldPath> knownFields,
            Map<FormulaFieldPath, FormulaFieldDefinition> fieldDefinitions
    ) {
        this.main = new RowValue(main == null ? new LinkedHashMap<>() : main);
        this.tables = new LinkedHashMap<>();
        if (tables != null) {
            tables.forEach((key, rows) -> this.tables.put(key, toRows(rows)));
        }
        this.strictFields = strictFields;
        this.typedFields = typedFields;
        this.knownFields = knownFields == null ? Set.of() : Set.copyOf(knownFields);
        this.knownTables = this.knownFields.stream()
                .map(FormulaFieldPath::tableKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        this.fieldDefinitions = fieldDefinitions == null ? Map.of() : Map.copyOf(fieldDefinitions);
    }

    public static FormulaRuntimeData of(Map<String, Object> main) {
        return new FormulaRuntimeData(main, Map.of());
    }

    public static FormulaRuntimeData of(Map<String, Object> main, Map<String, List<Map<String, Object>>> tables) {
        return new FormulaRuntimeData(main, tables);
    }

    public static FormulaRuntimeData strict(
            Map<String, Object> main,
            Map<String, List<Map<String, Object>>> tables,
            Set<String> knownDataIndexes
    ) {
        Set<FormulaFieldPath> fields = new HashSet<>();
        if (knownDataIndexes != null) {
            knownDataIndexes.forEach(dataIndex -> fields.add(FormulaFieldPath.parse(dataIndex)));
        }
        return new FormulaRuntimeData(main, tables, true, false, fields, Map.of());
    }

    public static FormulaRuntimeData typed(
            Map<String, Object> main,
            Map<String, List<Map<String, Object>>> tables,
            Collection<FormulaFieldDefinition> fields
    ) {
        Map<FormulaFieldPath, FormulaFieldDefinition> definitions = fields == null
                ? Map.of()
                : fields.stream().collect(Collectors.toMap(
                        FormulaFieldDefinition::fieldPath,
                        field -> field,
                        (left, right) -> {
                            throw new IllegalArgumentException(
                                    "duplicate formula field definition: " + left.fieldPath().dataIndex()
                            );
                        },
                        LinkedHashMap::new
                ));
        return new FormulaRuntimeData(main, tables, true, true, definitions.keySet(), definitions);
    }

    @Override
    public Object get(FormulaFieldPath fieldPath, FormulaEvaluationScope scope) {
        requireKnown(fieldPath);
        if (fieldPath.tableKey() == null) {
            return main.get(fieldPath.fieldName(), strictFields && knownFields.isEmpty());
        }
        if (scope.row() == null || !Objects.equals(scope.tableKey(), fieldPath.tableKey())) {
            return null;
        }
        return requireRow(scope.row()).get(fieldPath.fieldName(), strictFields && knownFields.isEmpty());
    }

    @Override
    public FormulaFieldWriteResult set(FormulaFieldPath fieldPath, Object value, FormulaEvaluationScope scope) {
        requireKnown(fieldPath);
        FormulaFieldDefinition fieldDefinition = fieldDefinitions.get(fieldPath);
        if (fieldDefinition != null && !fieldDefinition.writable()) {
            throw new FormulaEvaluationException(
                    "FORMULA_FIELD_NOT_WRITABLE",
                    fieldPath.dataIndex(),
                    "formula field is not writable: " + fieldPath.dataIndex()
            );
        }
        Object writeValue = FormulaValueConverter.convertForWrite(fieldDefinition, value);
        if (fieldPath.tableKey() == null) {
            return new FormulaFieldWriteResult(
                    fieldPath,
                    main.set(fieldPath.fieldName(), writeValue, strictFields && knownFields.isEmpty())
            );
        }
        if (scope.row() == null || !Objects.equals(scope.tableKey(), fieldPath.tableKey())) {
            throw new FormulaEvaluationException(
                    "FORMULA_FIELD_NOT_WRITABLE",
                    fieldPath.dataIndex(),
                    "formula field is not writable in current scope: " + fieldPath.dataIndex()
            );
        }
        return new FormulaFieldWriteResult(
                fieldPath,
                requireRow(scope.row()).set(fieldPath.fieldName(), writeValue, strictFields && knownFields.isEmpty())
        );
    }

    @Override
    public List<RowValue> rows(String tableKey) {
        if (strictFields && !tables.containsKey(tableKey) && !knownTables.contains(tableKey)) {
            throw new FormulaEvaluationException(
                    "FORMULA_UNKNOWN_TABLE",
                    tableKey,
                    "unknown formula table: " + tableKey
            );
        }
        return tables.getOrDefault(tableKey, List.of());
    }

    private void requireKnown(FormulaFieldPath fieldPath) {
        if (strictFields && !knownFields.isEmpty() && !knownFields.contains(fieldPath)) {
            throw new FormulaEvaluationException(
                    "FORMULA_UNKNOWN_FIELD",
                    fieldPath.dataIndex(),
                    "unknown formula field: " + fieldPath.dataIndex()
            );
        }
        if (strictFields && typedFields && !fieldDefinitions.containsKey(fieldPath)) {
            throw new FormulaEvaluationException(
                    "FORMULA_UNKNOWN_FIELD",
                    fieldPath.dataIndex(),
                    "unknown formula field: " + fieldPath.dataIndex()
            );
        }
    }

    private RowValue requireRow(Object row) {
        if (row instanceof RowValue rowValue) {
            return rowValue;
        }
        throw new FormulaEvaluationException("FORMULA_UNSUPPORTED_ROW", "unsupported formula row: " + row);
    }

    private static List<RowValue> toRows(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<RowValue> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            result.add(new RowValue(row == null ? new LinkedHashMap<>() : row));
        }
        return result;
    }

    static final class RowValue {
        private final Map<String, Object> values;

        RowValue(Map<String, Object> values) {
            this.values = values;
        }

        Object get(String key, boolean strictFields) {
            if (strictFields && !values.containsKey(key)) {
                throw new FormulaEvaluationException("FORMULA_UNKNOWN_FIELD", key, "unknown formula field: " + key);
            }
            return values.get(key);
        }

        boolean set(String key, Object value, boolean strictFields) {
            if (strictFields && !values.containsKey(key)) {
                throw new FormulaEvaluationException("FORMULA_UNKNOWN_FIELD", key, "unknown formula field: " + key);
            }
            Object old = values.get(key);
            if (Objects.equals(old, value)) {
                return false;
            }
            values.put(key, value);
            return true;
        }
    }
}
