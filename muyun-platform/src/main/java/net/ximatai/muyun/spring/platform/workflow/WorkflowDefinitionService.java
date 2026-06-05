package net.ximatai.muyun.spring.platform.workflow;

import net.ximatai.muyun.database.core.orm.Criteria;
import net.ximatai.muyun.spring.ability.AbstractAbilityService;
import net.ximatai.muyun.spring.ability.BaseDao;
import net.ximatai.muyun.spring.ability.EnableAbility;
import net.ximatai.muyun.spring.ability.SoftDeleteAbility;
import net.ximatai.muyun.spring.ability.SortAbility;
import net.ximatai.muyun.spring.common.util.PlatformNameRules;
import org.springframework.stereotype.Service;

@Service
public class WorkflowDefinitionService extends AbstractAbilityService<WorkflowDefinition> implements
        SoftDeleteAbility<WorkflowDefinition>,
        EnableAbility<WorkflowDefinition>,
        SortAbility<WorkflowDefinition> {
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
