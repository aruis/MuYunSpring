package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldQueryDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Table(name = "platform_metadata_field", comment = "Platform metadata field")
public class MetadataField extends StandardEnabledSortableEntity {
    @Column(name = "metadata_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Metadata id")
    private String metadataId;

    @Column(name = "field_name", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Field name")
    private String fieldName;

    @Column(name = "column_name", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Column name")
    private String columnName;

    @Column(name = "field_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Field type")
    private FieldType fieldType;

    @Column(name = "required", type = ColumnType.BOOLEAN, comment = "Required flag")
    private Boolean required;

    @Column(name = "unique_field", type = ColumnType.BOOLEAN, comment = "Unique field flag")
    private Boolean uniqueField;

    @Column(name = "indexed", type = ColumnType.BOOLEAN, comment = "Indexed flag")
    private Boolean indexed;

    @Column(name = "sortable_field", type = ColumnType.BOOLEAN, comment = "Sortable field flag")
    private Boolean sortableField;

    @Column(name = "title_field", type = ColumnType.BOOLEAN, comment = "Title field flag")
    private Boolean titleField;

    @Column(name = "field_length", type = ColumnType.INT, comment = "Field length")
    private Integer fieldLength;

    @Column(name = "precision_value", type = ColumnType.INT, comment = "Decimal precision")
    private Integer precision;

    @Column(name = "scale_value", type = ColumnType.INT, comment = "Decimal scale")
    private Integer scale;

    @Column(name = "dictionary_application_alias", type = ColumnType.VARCHAR, length = 64, comment = "Dictionary application alias")
    private String dictionaryApplicationAlias;

    @Column(name = "dictionary_category_alias", type = ColumnType.VARCHAR, length = 64, comment = "Dictionary category alias")
    private String dictionaryCategoryAlias;

    @Column(name = "queryable", type = ColumnType.BOOLEAN, comment = "Queryable flag")
    private Boolean queryable;

    @Column(name = "default_query_operator", type = ColumnType.VARCHAR, length = 32, comment = "Default query operator")
    private DynamicQueryOperator defaultQueryOperator;

    @Column(name = "query_operators", type = ColumnType.VARCHAR, length = 256, comment = "Allowed query operators")
    private String queryOperators;

    public FieldDefinition toDefinition() {
        FieldDefinition definition = new FieldDefinition(fieldName, columnName, fieldType, getTitle(),
                Boolean.TRUE.equals(required),
                Boolean.TRUE.equals(uniqueField),
                Boolean.TRUE.equals(indexed),
                Boolean.TRUE.equals(sortableField),
                Boolean.TRUE.equals(titleField),
                fieldLength,
                precision,
                scale,
                null,
                queryDefinition());
        if (dictionaryApplicationAlias != null && !dictionaryApplicationAlias.isBlank()
                && dictionaryCategoryAlias != null && !dictionaryCategoryAlias.isBlank()) {
            definition = definition.dictionary(dictionaryApplicationAlias, dictionaryCategoryAlias);
        }
        return definition;
    }

    private FieldQueryDefinition queryDefinition() {
        if (!Boolean.TRUE.equals(queryable)) {
            return FieldQueryDefinition.disabled();
        }
        Set<DynamicQueryOperator> operators = queryOperators == null || queryOperators.isBlank()
                ? DynamicQueryOperator.defaultOperators(fieldType)
                : Arrays.stream(queryOperators.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(DynamicQueryOperator::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        DynamicQueryOperator operator = defaultQueryOperator == null
                ? DynamicQueryOperator.defaultOperator(fieldType)
                : defaultQueryOperator;
        return FieldQueryDefinition.enabled(fieldType, operator, operators);
    }
}
