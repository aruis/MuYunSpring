package net.ximatai.muyun.spring.iam.organization;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.StandardTreeEntity;

@Getter
@Setter
@Table(name = "iam_organization", comment = "Organization")
public class Organization extends StandardTreeEntity {
    @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false, unique = true, comment = "Organization code")
    private String code;
}
