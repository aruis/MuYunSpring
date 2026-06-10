package net.ximatai.muyun.spring.platform.ui;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;
import net.ximatai.muyun.spring.dynamic.metadata.DynamicQueryOperator;

@Getter
@Setter
@Table(name = "platform_query_item", comment = "Platform query item")
public class PlatformQueryItem extends StandardEnabledTreeEntity {
    @Column(name = "query_template_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Query template id")
    private String queryTemplateId;

    @Column(name = "group_operator", type = ColumnType.VARCHAR, length = 16, comment = "Group operator")
    private PlatformQueryGroupOperator groupOperator;

    @Column(name = "module_metadata_field_id", type = ColumnType.VARCHAR, length = 32, comment = "Module metadata field id")
    private String moduleMetadataFieldId;

    @Column(name = "operator", type = ColumnType.VARCHAR, length = 32, comment = "Query operator")
    private DynamicQueryOperator operator;

    @Column(name = "default_value", type = ColumnType.VARCHAR, length = 512, comment = "Default value")
    private String defaultValue;

    @Column(name = "allow_external_value", type = ColumnType.BOOLEAN, comment = "Allow external value override",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean allowExternalValue = Boolean.FALSE;

    @Column(name = "external_value_key", type = ColumnType.VARCHAR, length = 64, comment = "External value key")
    private String externalValueKey;

    @Column(name = "time_zone", type = ColumnType.VARCHAR, length = 64, comment = "Query time zone")
    private String timeZone;
}
