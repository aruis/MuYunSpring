package net.ximatai.muyun.spring.platform.workflow;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEnabledSortableEntity;

@Getter
@Setter
@Table(name = "platform_workflow_task_check", comment = "Workflow task check")
@CompositeIndex(columns = {"task_definition_id", "check_key"}, unique = true)
public class WorkflowTaskCheck extends StandardEnabledSortableEntity {
    @Column(name = "task_definition_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Task definition id")
    private String taskDefinitionId;

    @Column(name = "check_key", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Check key")
    private String checkKey;

    @Column(name = "check_kind", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Check kind")
    private WorkflowTaskCheckKind checkKind;

    @Column(name = "expression", type = ColumnType.TEXT, comment = "Check expression")
    private String expression;

    @Column(name = "failure_message", type = ColumnType.VARCHAR, length = 256, comment = "Failure message")
    private String failureMessage;

    @Column(name = "check_config_text", type = ColumnType.TEXT, comment = "Check config")
    private String checkConfigText;
}
