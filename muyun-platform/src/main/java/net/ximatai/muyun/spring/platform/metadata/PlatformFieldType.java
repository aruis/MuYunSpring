package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;
import net.ximatai.muyun.spring.dynamic.metadata.FieldQueryDefinition;
import net.ximatai.muyun.spring.dynamic.metadata.FieldType;

import java.util.Set;

@Getter
@Setter
@Table(name = "platform_field_type", comment = "Platform field type")
@CompositeIndex(columns = {"alias"}, unique = true)
public class PlatformFieldType extends StandardEnabledSortableEntity {
    @Column(name = "alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Field type alias")
    private String alias;

    @Column(name = "title", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Field type title")
    private String title;

    @Column(name = "field_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Runtime field type")
    private FieldType fieldType;

    @Column(name = "default_length", type = ColumnType.INT, comment = "Default length")
    private Integer defaultLength;

    @Column(name = "default_precision", type = ColumnType.INT, comment = "Default decimal precision")
    private Integer defaultPrecision;

    @Column(name = "default_scale", type = ColumnType.INT, comment = "Default decimal scale")
    private Integer defaultScale;

    @Column(name = "default_query_operator", type = ColumnType.VARCHAR, length = 32, comment = "Default query operator")
    private DynamicQueryOperator defaultQueryOperator;

    @Column(name = "query_operators", type = ColumnType.JSON_SET, comment = "Allowed query operators")
    private Set<String> queryOperators;

    @Column(name = "default_ui_type_alias", type = ColumnType.VARCHAR, length = 64, comment = "Default field UI type alias")
    private String defaultUiTypeAlias;

    @Column(name = "ui_type_aliases", type = ColumnType.JSON_SET, comment = "Allowed field UI type aliases")
    private Set<String> uiTypeAliases;

    public FieldQueryDefinition queryDefinition() {
        if (defaultQueryOperator == null && (queryOperators == null || queryOperators.isEmpty())) {
            return FieldQueryDefinition.disabled();
        }
        DynamicQueryOperator operator = defaultQueryOperator == null
                ? DynamicQueryOperator.defaultOperator(fieldType)
                : defaultQueryOperator;
        Set<DynamicQueryOperator> operators = queryOperators == null || queryOperators.isEmpty()
                ? DynamicQueryOperator.defaultOperators(fieldType)
                : DynamicQueryOperator.parseNames(queryOperators);
        return FieldQueryDefinition.enabled(fieldType, operator, operators);
    }
}
