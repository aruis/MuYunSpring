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
public class WorkflowTaskDefinitionService extends AbstractAbilityService<WorkflowTaskDefinition> implements
        SoftDeleteAbility<WorkflowTaskDefinition>,
        EnableAbility<WorkflowTaskDefinition>,
        SortAbility<WorkflowTaskDefinition> {
    public static final String MODULE_ALIAS = "platform.workflow.task-definition";

    public WorkflowTaskDefinitionService(BaseDao<WorkflowTaskDefinition, String> workflowTaskDefinitionDao) {
        super(MODULE_ALIAS, WorkflowTaskDefinition.class, workflowTaskDefinitionDao);
    }

    @Override
    public void beforeInsert(WorkflowTaskDefinition taskDefinition) {
        normalizeAndValidate(taskDefinition);
    }

    @Override
    public void beforeUpdate(WorkflowTaskDefinition taskDefinition) {
        normalizeAndValidate(taskDefinition);
    }

    @Override
    public Criteria sortScope(WorkflowTaskDefinition taskDefinition) {
        return Criteria.of().eq("moduleAlias", taskDefinition.getModuleAlias());
    }

    private void normalizeAndValidate(WorkflowTaskDefinition taskDefinition) {
        taskDefinition.setModuleAlias(PlatformNameRules.requireModuleAlias(taskDefinition.getModuleAlias()));
        taskDefinition.setAlias(PlatformNameRules.requireIdentifier(taskDefinition.getAlias(), "workflowTaskAlias"));
        if (taskDefinition.getManualConfirm() == null) {
            taskDefinition.setManualConfirm(Boolean.TRUE);
        }
        rejectDuplicate(taskDefinition, Criteria.of()
                        .eq("moduleAlias", taskDefinition.getModuleAlias())
                        .eq("alias", taskDefinition.getAlias()),
                "workflowTaskAlias must be unique within module: " + taskDefinition.getAlias());
    }
}
