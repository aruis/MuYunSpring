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
@Table(name = "platform_dictionary_item", comment = "Platform dictionary item")
@CompositeIndex(columns = {"tenant_id", "category_id", "code"}, unique = true)
public class DictionaryItem extends StandardEnabledTreeEntity {
    @Column(name = "category_id", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Dictionary category id")
    private String categoryId;

    @Column(name = "category_alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Dictionary category alias")
    private String categoryAlias;

    @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Dictionary item code")
    private String code;
}
