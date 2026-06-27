package net.ximatai.muyun.spring.dynamic.descriptor;

import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QuerySchema;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DynamicQuerySchemas {
    private DynamicQuerySchemas() {
    }

    public static QuerySchema from(String moduleAlias, DynamicEntityDescriptor descriptor, List<String> quickSearchFields) {
        Set<String> quickSearchFieldSet = quickSearchFields == null
                ? Set.of()
                : new LinkedHashSet<>(quickSearchFields);
        List<QuerySchema.Field> fields = descriptor.fields().stream()
                .filter(DynamicQuerySchemas::queryable)
                .map(field -> field(field, quickSearchFieldSet.contains(field.fieldName())))
                .toList();
        List<QuerySchema.Field> quickSearchFieldSchemas = descriptor.fields().stream()
                .filter(field -> quickSearchFieldSet.contains(field.fieldName()))
                .map(DynamicQuerySchemas::quickSearchField)
                .toList();
        return new QuerySchema(
                moduleAlias,
                descriptor.entityAlias(),
                new QuerySchema.QuickSearch(
                        !quickSearchFieldSchemas.isEmpty(),
                        quickSearchFieldSchemas.stream().map(QuerySchema.Field::name).toList(),
                        quickSearchFieldSchemas
                ),
                fields,
                List.of(),
                List.of()
        );
    }

    private static QuerySchema.Field field(DynamicFieldDescriptor field, boolean quickSearch) {
        boolean queryable = queryable(field);
        return new QuerySchema.Field(
                field.fieldName(),
                field.title(),
                valueType(field.type()),
                queryable ? operators(field) : List.of(QueryOperator.LIKE),
                queryable ? QueryOperator.valueOf(field.query().defaultOperator()) : QueryOperator.LIKE,
                quickSearch,
                field.sortable()
        );
    }

    private static QuerySchema.Field quickSearchField(DynamicFieldDescriptor field) {
        return new QuerySchema.Field(
                field.fieldName(),
                field.title(),
                valueType(field.type()),
                List.of(QueryOperator.LIKE),
                QueryOperator.LIKE,
                true,
                field.sortable()
        );
    }

    private static boolean queryable(DynamicFieldDescriptor field) {
        return field.query() != null && field.query().queryable();
    }

    private static List<QueryOperator> operators(DynamicFieldDescriptor field) {
        return field.query().operators().stream()
                .map(QueryOperator::valueOf)
                .toList();
    }

    private static QueryValueType valueType(FieldType type) {
        return switch (type) {
            case STRING -> QueryValueType.STRING;
            case TEXT -> QueryValueType.TEXT;
            case BOOLEAN -> QueryValueType.BOOLEAN;
            case INTEGER -> QueryValueType.INTEGER;
            case LONG -> QueryValueType.LONG;
            case DECIMAL -> QueryValueType.DECIMAL;
            case TIMESTAMP, ZONED_TIMESTAMP -> QueryValueType.INSTANT;
            case DATE -> QueryValueType.DATE;
            case JSON -> QueryValueType.JSON;
        };
    }
}
