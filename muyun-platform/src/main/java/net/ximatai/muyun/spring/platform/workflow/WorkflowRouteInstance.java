package net.ximatai.muyun.spring.platform.workflow;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardEntity;

import java.time.Instant;

@Getter
@Setter
@Table(name = "platform_workflow_route_instance", comment = "Workflow route instance")
@CompositeIndex(columns = {"instance_id", "route_key"})
@CompositeIndex(columns = {"instance_id", "route_status"})
public class WorkflowRouteInstance extends StandardEntity {
    @Column(name = "instance_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Workflow instance id")
    private String instanceId;

    @Column(name = "route_key", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Route key")
    private String routeKey;

    @Column(name = "route_run_id", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Route run id")
    private String routeRunId;

    @Column(name = "source_node_key", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Source node key")
    private String sourceNodeKey;

    @Column(name = "target_node_key", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Target node key")
    private String targetNodeKey;

    @Column(name = "branch_node_key", type = ColumnType.VARCHAR, length = 64, comment = "Branch node key")
    private String branchNodeKey;

    @Column(name = "branch_run_id", type = ColumnType.VARCHAR, length = 64, comment = "Branch run id")
    private String branchRunId;

    @Column(name = "converge_node_key", type = ColumnType.VARCHAR, length = 64, comment = "Converge node key")
    private String convergeNodeKey;

    @Column(name = "converge_run_id", type = ColumnType.VARCHAR, length = 64, comment = "Converge run id")
    private String convergeRunId;

    @Column(name = "parent_route_id", type = ColumnType.VARCHAR, length = 32, comment = "Parent route id")
    private String parentRouteId;

    @Column(name = "route_depth", type = ColumnType.INT, comment = "Route depth")
    private Integer routeDepth;

    @Column(name = "route_status", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Route status", defaultVal = @Default(varchar = "candidate"))
    private WorkflowRouteStatus routeStatus = WorkflowRouteStatus.CANDIDATE;

    @Column(name = "route_reason", type = ColumnType.VARCHAR, length = 32, comment = "Route reason")
    private WorkflowRouteReason routeReason;

    @Column(name = "condition_matched", type = ColumnType.BOOLEAN, nullable = false,
            comment = "Whether condition matched", defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean conditionMatched = Boolean.FALSE;

    @Column(name = "default_route", type = ColumnType.BOOLEAN, nullable = false, comment = "Default route",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean defaultRoute = Boolean.FALSE;

    @Column(name = "selected_by", type = ColumnType.VARCHAR, length = 64, comment = "Selected by")
    private String selectedBy;

    @Column(name = "selected_at", type = ColumnType.TIMESTAMP, comment = "Selected at")
    private Instant selectedAt;

    @Column(name = "arrived_at", type = ColumnType.TIMESTAMP, comment = "Arrived at converge")
    private Instant arrivedAt;

    @Column(name = "closed_by_route_id", type = ColumnType.VARCHAR, length = 32, comment = "Closed by route id")
    private String closedByRouteId;

    @Column(name = "closed_reason", type = ColumnType.VARCHAR, length = 256, comment = "Closed reason")
    private String closedReason;

    @Column(name = "invalidated_by_action_id", type = ColumnType.VARCHAR, length = 64,
            comment = "Invalidated by action id")
    private String invalidatedByActionId;

    @Column(name = "invalidated_at", type = ColumnType.TIMESTAMP, comment = "Invalidated at")
    private Instant invalidatedAt;
}
