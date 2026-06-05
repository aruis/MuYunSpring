package net.ximatai.muyun.spring.platform.workflow;

import lombok.Getter;
import lombok.Setter;
import net.ximatai.muyun.database.core.annotation.Column;
import net.ximatai.muyun.database.core.annotation.CompositeIndex;
import net.ximatai.muyun.database.core.annotation.Default;
import net.ximatai.muyun.database.core.annotation.Table;
import net.ximatai.muyun.database.core.annotation.TrueOrFalse;
import net.ximatai.muyun.database.core.builder.ColumnType;
import net.ximatai.muyun.spring.common.model.standard.StandardSortableEntity;

@Getter
@Setter
@Table(name = "platform_workflow_link_definition", comment = "Workflow link definition")
@CompositeIndex(columns = {"workflow_version_id", "route_key"}, unique = true)
public class WorkflowLinkDefinition extends StandardSortableEntity {
    @Column(name = "workflow_version_id", type = ColumnType.VARCHAR, length = 32, nullable = false,
            comment = "Workflow version id")
    private String workflowVersionId;

    @Column(name = "route_key", type = ColumnType.VARCHAR, length = 64, nullable = false, comment = "Route key")
    private String routeKey;

    @Column(name = "source_node_key", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Source node key")
    private String sourceNodeKey;

    @Column(name = "target_node_key", type = ColumnType.VARCHAR, length = 64, nullable = false,
            comment = "Target node key")
    private String targetNodeKey;

    @Column(name = "condition_expression", type = ColumnType.TEXT, comment = "Route condition expression")
    private String conditionExpression;

    @Column(name = "default_route", type = ColumnType.BOOLEAN, nullable = false, comment = "Default route",
            defaultVal = @Default(bool = TrueOrFalse.FALSE))
    private Boolean defaultRoute = Boolean.FALSE;

    @Column(name = "route_config_text", type = ColumnType.TEXT, comment = "Route config")
    private String routeConfigText;
}
