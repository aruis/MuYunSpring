package net.ximatai.muyun.spring.platform.workflow;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

@Getter
@Setter
@Table(name = "platform_workflow_node_definition", comment = "Workflow node definition")
@CompositeIndex(columns = {"workflow_version_id", "node_key"}, unique = true)
public class WorkflowNodeDefinition extends StandardSortableEntity {
    @Column(name = "workflow_version_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Workflow version id")
    private String workflowVersionId;

    @Column(name = "node_key", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Node key")
    private String nodeKey;

    @Column(name = "node_type", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Node type")
    private WorkflowNodeType nodeType;

    @Column(name = "approval_mode", type = ColumnType.VARCHAR, length = 32, comment = "Approval mode")
    private WorkflowApprovalMode approvalMode;

    @Column(name = "milestone_type", type = ColumnType.VARCHAR, length = 32, comment = "Milestone type")
    private WorkflowMilestoneType milestoneType;

    @Column(name = "converge_mode", type = ColumnType.VARCHAR, length = 32, comment = "Converge mode")
    private WorkflowConvergeMode convergeMode;

    @Column(name = "converge_ratio", type = ColumnType.INT, comment = "Converge ratio percent")
    private Integer convergeRatio;

    @Column(name = "task_definition_id", type = ColumnType.VARCHAR, length = 32, comment = "Task definition id")
    private String taskDefinitionId;

    @Column(name = "participant_policy_text", type = ColumnType.TEXT, comment = "Participant policy")
    private String participantPolicyText;

    @Column(name = "node_config_text", type = ColumnType.TEXT, comment = "Node config")
    private String nodeConfigText;
}
