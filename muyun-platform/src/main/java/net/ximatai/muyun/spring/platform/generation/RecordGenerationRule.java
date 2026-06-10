package net.ximatai.muyun.spring.platform.generation;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.Indexed;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Table(name = "platform_record_generation_rule", comment = "Platform record generation rule")
public class RecordGenerationRule extends StandardEnabledSortableEntity {
    @Indexed
    @Column(name = "source_module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Source module alias")
    private String sourceModuleAlias;

    @Indexed
    @Column(name = "target_module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false,
            comment = "Target module alias")
    private String targetModuleAlias;

    @Column(name = "action_code", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Generated action code")
    private String actionCode;

    @Column(name = "generation_condition", type = ColumnType.TEXT, comment = "Generation condition expression")
    private String generationCondition;

    private transient List<RecordGenerationObjectMapping> objectMappings = new ArrayList<>();
}
