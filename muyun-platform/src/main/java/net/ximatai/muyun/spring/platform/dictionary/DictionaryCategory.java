package net.ximatai.muyun.spring.platform.dictionary;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;

@Getter
@Setter
@Table(name = "platform_dictionary_category", comment = "Platform dictionary category")
@CompositeIndex(columns = {"tenant_id", "application_alias", "alias"}, unique = true)
public class DictionaryCategory extends StandardEnabledTreeEntity {
    @Column(name = "application_alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Application alias")
    private String applicationAlias;

    @Column(name = "alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Dictionary category alias")
    private String alias;

    @Column(name = "category_kind", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Dictionary category kind")
    private DictionaryCategoryKind categoryKind;
}
