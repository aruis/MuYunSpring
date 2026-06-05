package net.ximatai.muyun.spring.platform.workflow;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

import java.time.Instant;

@Getter
@Setter
@Table(name = "platform_workflow_task", comment = "Workflow task")
@CompositeIndex(columns = {"tenant_id", "assignee_id", "task_status"})
@CompositeIndex(columns = {"instance_id", "node_instance_id"})
@CompositeIndex(columns = {"instance_id", "task_status"})
public class WorkflowTask extends StandardEntity {
    @Column(name = "instance_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Workflow instance id")
    private String instanceId;

    @Column(name = "node_instance_id", type = ColumnType.VARCHAR, length = 32, comment = "Workflow node instance id")
    private String nodeInstanceId;

    @Column(name = "task_kind", type = ColumnType.VARCHAR, length = 32, nullable = false, comment = "Task kind")
    private WorkflowTaskKind taskKind;

    @Column(name = "task_status", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Task status", defaultVal = @Default(varchar = "todo"))
    private WorkflowTaskStatus taskStatus = WorkflowTaskStatus.TODO;

    @Column(name = "parent_task_id", type = ColumnType.VARCHAR, length = 32, comment = "Parent task id")
    private String parentTaskId;

    @Column(name = "origin_task_id", type = ColumnType.VARCHAR, length = 32, comment = "Origin task id")
    private String originTaskId;

    @Column(name = "assignment_kind", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Assignment kind", defaultVal = @Default(varchar = "normal"))
    private WorkflowAssignmentKind assignmentKind = WorkflowAssignmentKind.NORMAL;

    @Column(name = "add_sign_mode", type = ColumnType.VARCHAR, length = 32, comment = "Add sign mode")
    private WorkflowAddSignMode addSignMode;

    @Column(name = "owner_id", type = ColumnType.VARCHAR, length = 64, comment = "Original owner id")
    private String ownerId;

    @Column(name = "original_assignee_id", type = ColumnType.VARCHAR, length = 64, comment = "Original assignee id")
    private String originalAssigneeId;

    @Column(name = "assignee_id", type = ColumnType.VARCHAR, length = 64, comment = "Current assignee id")
    private String assigneeId;

    @Column(name = "actual_processor_id", type = ColumnType.VARCHAR, length = 64, comment = "Actual processor id")
    private String actualProcessorId;

    @Column(name = "delegated_from_user_id", type = ColumnType.VARCHAR, length = 64,
            comment = "Delegated from user id")
    private String delegatedFromUserId;

    @Column(name = "transferred_from_user_id", type = ColumnType.VARCHAR, length = 64,
            comment = "Transferred from user id")
    private String transferredFromUserId;

    @Column(name = "transferred_by", type = ColumnType.VARCHAR, length = 64, comment = "Transferred by")
    private String transferredBy;

    @Column(name = "transferred_at", type = ColumnType.TIMESTAMP, comment = "Transferred at")
    private Instant transferredAt;

    @Column(name = "added_by", type = ColumnType.VARCHAR, length = 64, comment = "Added by")
    private String addedBy;

    @Column(name = "added_at", type = ColumnType.TIMESTAMP, comment = "Added at")
    private Instant addedAt;

    @Column(name = "decision", type = ColumnType.VARCHAR, length = 64, comment = "Task decision")
    private String decision;

    @Column(name = "check_status", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Task check status", defaultVal = @Default(varchar = "not_checked"))
    private WorkflowTaskCheckStatus checkStatus = WorkflowTaskCheckStatus.NOT_CHECKED;

    @Column(name = "check_result_text", type = ColumnType.TEXT, comment = "Task check result")
    private String checkResultText;

    @Column(name = "result_message", type = ColumnType.TEXT, comment = "Task result message")
    private String resultMessage;

    @Column(name = "assignment_policy_text", type = ColumnType.TEXT, comment = "Assignment policy")
    private String assignmentPolicyText;

    @Column(name = "assignment_snapshot_text", type = ColumnType.TEXT, comment = "Assignment snapshot")
    private String assignmentSnapshotText;

    @Column(name = "delegation_policy_id", type = ColumnType.VARCHAR, length = 64, comment = "Delegation policy id")
    private String delegationPolicyId;

    @Column(name = "due_at", type = ColumnType.TIMESTAMP, comment = "Due at")
    private Instant dueAt;

    @Column(name = "completed_at", type = ColumnType.TIMESTAMP, comment = "Completed at")
    private Instant completedAt;
}
