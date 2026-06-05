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
@Table(name = "platform_workflow_task_guide", comment = "Workflow task guide")
@CompositeIndex(columns = {"task_definition_id", "guide_key"}, unique = true)
public class WorkflowTaskGuide extends StandardEnabledSortableEntity {
    @Column(name = "task_definition_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Task definition id")
    private String taskDefinitionId;

    @Column(name = "guide_key", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Guide key")
    private String guideKey;

    @Column(name = "guide_kind", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Guide kind")
    private WorkflowTaskGuideKind guideKind;

    @Column(name = "target_module_alias", type = ColumnType.VARCHAR, length = 128, comment = "Target module alias")
    private String targetModuleAlias;

    @Column(name = "target_action_code", type = ColumnType.VARCHAR, length = 64, comment = "Target action code")
    private String targetActionCode;

    @Column(name = "guide_config_text", type = ColumnType.TEXT, comment = "Guide config")
    private String guideConfigText;
}
