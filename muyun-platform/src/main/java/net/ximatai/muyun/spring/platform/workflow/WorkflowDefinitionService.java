package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.database.core.orm.Sort;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.ability.query.QueryAbility;
import net.ximatai.muyun.spring.ability.query.QueryDescriptor;
import net.ximatai.muyun.spring.ability.query.QueryField;
import net.ximatai.muyun.spring.ability.query.QueryOperator;
import net.ximatai.muyun.spring.ability.query.QueryValueType;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

@Service
public class WorkflowDefinitionService extends AbstractAbilityService<WorkflowDefinition> implements
        SoftDeleteAbility<WorkflowDefinition>,
        EnableAbility<WorkflowDefinition>,
        SortAbility<WorkflowDefinition>,
        QueryAbility<WorkflowDefinition> {
    public static final String MODULE_ALIAS = "platform.workflow.definition";

    public WorkflowDefinitionService(BaseDao<WorkflowDefinition, String> workflowDefinitionDao) {
        super(MODULE_ALIAS, WorkflowDefinition.class, workflowDefinitionDao);
    }

    @Override
    public void beforeInsert(WorkflowDefinition definition) {
        normalizeAndValidate(definition);
    }

    @Override
    public void beforeUpdate(WorkflowDefinition definition) {
        normalizeAndValidate(definition);
    }

    @Override
    public Criteria sortScope(WorkflowDefinition definition) {
        return Criteria.of().eq("moduleAlias", definition.getModuleAlias());
    }

    @Override
    public QueryDescriptor queryDescriptor() {
        return QueryDescriptor.builder(MODULE_ALIAS)
                .field(QueryField.of("id", QueryOperator.EQ, QueryOperator.IN).withTitle("ID"))
                .field(QueryField.of("applicationAlias", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("应用别名"))
                .field(QueryField.of("moduleAlias", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("模块别名"))
                .field(QueryField.of("alias", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.IN)
                        .withTitle("工作流别名"))
                .field(QueryField.of("approvalEnabled", QueryValueType.BOOLEAN, QueryOperator.EQ)
                        .withTitle("审批启用"))
                .field(QueryField.of("actionCode", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("动作编码"))
                .field(QueryField.of("definitionStatus", QueryValueType.STRING, QueryOperator.EQ)
                        .withTitle("定义状态"))
                .field(QueryField.of("currentVersionNo", QueryValueType.INTEGER, QueryOperator.EQ)
                        .withTitle("当前版本号"))
                .field(QueryField.of("title", QueryValueType.STRING, QueryOperator.EQ, QueryOperator.LIKE)
                        .withTitle("名称").withQuickSearch())
                .field(QueryField.of("enabled", QueryValueType.BOOLEAN, QueryOperator.EQ)
                        .withTitle("启用状态"))
                .field(QueryField.of("sortOrder", QueryValueType.INTEGER, QueryOperator.EQ)
                        .withTitle("排序号").withSortable())
                .field(QueryField.of("createdAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("创建时间").withSortable())
                .field(QueryField.of("updatedAt", QueryValueType.INSTANT, QueryOperator.GTE, QueryOperator.LTE,
                                QueryOperator.BETWEEN)
                        .withTitle("更新时间").withSortable())
                .defaultSort(Sort.asc("sortOrder"))
                .build();
    }

    private void normalizeAndValidate(WorkflowDefinition definition) {
        definition.setApplicationAlias(PlatformNameRules.requireApplicationAlias(definition.getApplicationAlias()));
        definition.setModuleAlias(PlatformNameRules.requireModuleAlias(definition.getModuleAlias()));
        definition.setAlias(PlatformNameRules.requireIdentifier(definition.getAlias(), "workflowAlias"));
        if (definition.getApprovalEnabled() == null) {
            definition.setApprovalEnabled(Boolean.FALSE);
        }
        if (definition.getActionCode() != null && definition.getActionCode().isBlank()) {
            definition.setActionCode(null);
        }
        if (definition.getActionCode() != null) {
            definition.setActionCode(PlatformNameRules.requireActionCode(definition.getActionCode(), "actionCode"));
        }
        if (definition.getDefinitionStatus() == null) {
            definition.setDefinitionStatus(WorkflowDefinitionStatus.DRAFT);
        }
        if (!Boolean.TRUE.equals(definition.getApprovalEnabled()) && definition.getActionCode() != null) {
            rejectDuplicate(definition, Criteria.of()
                            .eq("moduleAlias", definition.getModuleAlias())
                            .eq("approvalEnabled", Boolean.FALSE)
                            .eq("actionCode", definition.getActionCode()),
                    "workflow actionCode must be unique within module: " + definition.getActionCode());
        }
        rejectDuplicate(definition, Criteria.of()
                        .eq("moduleAlias", definition.getModuleAlias())
                        .eq("alias", definition.getAlias()),
                "workflowAlias must be unique within module: " + definition.getAlias());
    }
}
