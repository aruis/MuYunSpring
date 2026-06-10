package net.ximatai.muyun.spring.platform.metadata;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

import java.util.Set;

@Getter
@Setter
@Table(name = "platform_module_metadata_field", comment = "Module metadata field config")
@CompositeIndex(columns = {"relation_id", "metadata_field_id"}, unique = true)
public class ModuleMetadataField extends StandardSortableEntity {
    @Column(name = "relation_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Module metadata relation id")
    private String relationId;

    @Column(name = "metadata_field_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Metadata field id")
    private String metadataFieldId;

    @Column(name = "default_value", type = ColumnType.VARCHAR, length = 512, comment = "Default value")
    private String defaultValue;

    @Column(name = "cloneable", type = ColumnType.BOOLEAN, comment = "Cloneable flag",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean cloneable = Boolean.FALSE;

    @Column(name = "validation_regex", type = ColumnType.VARCHAR, length = 512, comment = "Validation regex")
    private String validationRegex;

    @Column(name = "dictionary_application_alias", type = ColumnType.VARCHAR, length = 64, comment = "Dictionary application alias")
    private String dictionaryApplicationAlias;

    @Column(name = "dictionary_category_alias", type = ColumnType.VARCHAR, length = 64, comment = "Dictionary category alias")
    private String dictionaryCategoryAlias;

    @Column(name = "reference_module_alias", type = ColumnType.VARCHAR, length = 128, comment = "Reference module alias")
    private String referenceModuleAlias;

    @Column(name = "reference_module_key_field", type = ColumnType.VARCHAR, length = 64, comment = "Reference module key field")
    private String referenceModuleKeyField;

    @Column(name = "reference_module_label_field", type = ColumnType.VARCHAR, length = 64, comment = "Reference module label field")
    private String referenceModuleLabelField;

    @Column(name = "reference_generate_rule_id", type = ColumnType.VARCHAR, length = 32, comment = "Reference generate rule id")
    private String referenceGenerateRuleId;

    @Column(name = "reference_query_template_id", type = ColumnType.VARCHAR, length = 32, comment = "Reference query template id")
    private String referenceQueryTemplateId;

    @Column(name = "reference_module_plus_fields", type = ColumnType.JSON_SET, comment = "Reference module plus fields")
    private Set<String> referenceModulePlusFields;
}
