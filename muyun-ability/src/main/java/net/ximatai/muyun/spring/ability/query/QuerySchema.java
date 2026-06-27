package net.ximatai.muyun.spring.ability.query;

import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.database.core.orm.SortDirection;

import java.util.Arrays;
import java.util.List;

public record QuerySchema(String scopeName,
                          String entityAlias,
                          QuickSearch quickSearch,
                          List<Field> fields,
                          List<ExternalCriteria> externalCriteria,
                          List<DefaultSort> defaultSorts) {
    public static final QuerySchema EMPTY = new QuerySchema(null, null, QuickSearch.disabled(), List.of(), List.of(), List.of());

    public QuerySchema {
        fields = fields == null ? List.of() : List.copyOf(fields);
        externalCriteria = externalCriteria == null ? List.of() : List.copyOf(externalCriteria);
        defaultSorts = defaultSorts == null ? List.of() : List.copyOf(defaultSorts);
        quickSearch = quickSearch == null ? QuickSearch.disabled() : quickSearch;
    }

    public static QuerySchema from(QueryDescriptor descriptor) {
        List<Field> fields = descriptor.fields().stream()
                .map(Field::from)
                .toList();
        return new QuerySchema(
                descriptor.scopeName(),
                null,
                QuickSearch.from(descriptor),
                fields,
                descriptor.externalCriteriaKeys().stream()
                        .map(ExternalCriteria::pageContextObject)
                        .toList(),
                Arrays.stream(descriptor.defaultSorts())
                        .map(DefaultSort::from)
                        .toList()
        );
    }

    public record QuickSearch(boolean enabled,
                              List<String> fields,
                              List<Field> fieldSchemas) {
        public QuickSearch {
            fields = fields == null ? List.of() : List.copyOf(fields);
            fieldSchemas = fieldSchemas == null ? List.of() : List.copyOf(fieldSchemas);
        }

        static QuickSearch from(QueryDescriptor descriptor) {
            List<Field> fieldSchemas = descriptor.quickSearchFields().stream()
                    .map(Field::from)
                    .toList();
            List<String> fields = fieldSchemas.stream().map(Field::name).toList();
            return new QuickSearch(!fields.isEmpty(), fields, fieldSchemas);
        }

        static QuickSearch disabled() {
            return new QuickSearch(false, List.of(), List.of());
        }
    }

    public record Field(String name,
                        String title,
                        QueryValueType valueType,
                        List<QueryOperator> operators,
                        QueryOperator defaultOperator,
                        boolean quickSearch,
                        boolean sortable) {
        public Field {
            operators = operators == null ? List.of() : List.copyOf(operators);
        }

        static Field from(QueryField field) {
            return new Field(
                    field.fieldName(),
                    field.title(),
                    field.valueType(),
                    List.copyOf(field.operators()),
                    field.defaultOperator(),
                    field.quickSearch(),
                    field.sortable()
            );
        }
    }

    public record ExternalCriteria(String key,
                                   String valueType,
                                   String providedBy) {
        static ExternalCriteria pageContextObject(String key) {
            return new ExternalCriteria(key, "OBJECT", "PAGE_CONTEXT");
        }
    }

    public record DefaultSort(String field,
                              boolean desc) {
        static DefaultSort from(Sort sort) {
            return new DefaultSort(sort.getField(), sort.getDirection() == SortDirection.DESC);
        }
    }
}
