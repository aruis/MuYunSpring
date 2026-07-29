package net.ximatai.muyun.spring.iam.position;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledTreeEntity;
import net.ximatai.muyun.spring.common.model.constraint.TenantUniqueConstraint;

@Getter
@Setter
@Table(name = "iam_position_category", comment = "Position category")
@TenantUniqueConstraint(fields = "code")
public class PositionCategory extends StandardEnabledTreeEntity {
    @Column(name = "code", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Position category code")
    private String code;

    @Column(name = "description", type = ColumnType.VARCHAR, length = 512, comment = "Description")
    private String description;
}
