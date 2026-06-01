package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldQueryDefinition;

import java.util.Set;

@Getter
@Setter
@Table(name = "platform_metadata_field_config", comment = "Metadata field config")
@CompositeIndex(columns = {"metadata_field_id", "relation_id"}, unique = true)
public class MetadataFieldConfig extends StandardEntity {
    @Column(name = "metadata_field_id", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Metadata field id")
    private String metadataFieldId;

    @Column(name = "relation_id", type = ColumnType.VARCHAR, length = 32, comment = "Module metadata relation id")
    private String relationId;

    @Column(name = "dictionary_application_alias", type = ColumnType.VARCHAR, length = 64, comment = "Dictionary application alias")
    private String dictionaryApplicationAlias;

    @Column(name = "dictionary_category_alias", type = ColumnType.VARCHAR, length = 64, comment = "Dictionary category alias")
    private String dictionaryCategoryAlias;

    @Column(name = "field_length", type = ColumnType.INT, comment = "Field length")
    private Integer fieldLength;

    @Column(name = "precision", type = ColumnType.INT, comment = "Decimal precision")
    private Integer precision;

    @Column(name = "scale", type = ColumnType.INT, comment = "Decimal scale")
    private Integer scale;

    @Column(name = "queryable", type = ColumnType.BOOLEAN, comment = "Queryable flag")
    private Boolean queryable;

    @Column(name = "default_query_operator", type = ColumnType.VARCHAR, length = 32, comment = "Default query operator")
    private DynamicQueryOperator defaultQueryOperator;

    @Column(name = "query_operators", type = ColumnType.JSON_SET, comment = "Allowed query operators")
    private Set<String> queryOperators;

    @Column(name = "default_value", type = ColumnType.VARCHAR, length = 512, comment = "Default value")
    private String defaultValue;

    @Column(name = "validation_regex", type = ColumnType.VARCHAR, length = 512, comment = "Validation regex")
    private String validationRegex;

    @Column(name = "copyable", type = ColumnType.BOOLEAN, comment = "Copyable flag")
    private Boolean copyable;

    @Column(name = "write_protected", type = ColumnType.BOOLEAN, comment = "Write protected flag")
    private Boolean writeProtected;

    public FieldQueryDefinition queryDefinition(PlatformFieldType fieldType) {
        if (queryable == null) {
            return fieldType.queryDefinition();
        }
        if (!queryable) {
            return FieldQueryDefinition.disabled();
        }
        DynamicQueryOperator operator = defaultQueryOperator == null
                ? DynamicQueryOperator.defaultOperator(fieldType.getFieldType())
                : defaultQueryOperator;
        Set<DynamicQueryOperator> operators = queryOperators == null || queryOperators.isEmpty()
                ? DynamicQueryOperator.defaultOperators(fieldType.getFieldType())
                : DynamicQueryOperator.parseNames(queryOperators);
        return FieldQueryDefinition.enabled(fieldType.getFieldType(), operator, operators);
    }

    public boolean hasDictionaryBinding() {
        return dictionaryCategoryAlias != null && !dictionaryCategoryAlias.isBlank();
    }

    public Integer effectiveLength(PlatformFieldType fieldType) {
        return fieldLength == null ? fieldType.getDefaultLength() : fieldLength;
    }

    public Integer effectivePrecision(PlatformFieldType fieldType) {
        return precision == null ? fieldType.getDefaultPrecision() : precision;
    }

    public Integer effectiveScale(PlatformFieldType fieldType) {
        return scale == null ? fieldType.getDefaultScale() : scale;
    }

    public boolean hasBehaviorDefinition() {
        return defaultValue != null
                || validationRegex != null
                || copyable != null
                || writeProtected != null;
    }
}
