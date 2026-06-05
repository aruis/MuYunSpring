package net.ximatai.muyun.spring.platform.workflow;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "platform_workflow_task_definition", comment = "Workflow task definition")
@CompositeIndex(columns = {"tenant_id", "module_alias", "alias"}, unique = true)
public class WorkflowTaskDefinition extends StandardEnabledSortableEntity {
    @Column(name = "module_alias", type = ColumnType.VARCHAR, length = 128, nullable = false, comment = "Module alias")
    private String moduleAlias;

    @Column(name = "entity_alias", type = ColumnType.VARCHAR, length = 64, comment = "Entity alias")
    private String entityAlias;

    @Column(name = "alias", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Task alias")
    private String alias;

    @Column(name = "manual_confirm", type = ColumnType.BOOLEAN, nullable = false,
            comment = "Whether task requires manual confirmation", defaultVal = @Default(bool = TrueOrFalse.TRUE))
    private Boolean manualConfirm = Boolean.TRUE;

    @Column(name = "task_config_text", type = ColumnType.TEXT, comment = "Task config")
    private String taskConfigText;
}
